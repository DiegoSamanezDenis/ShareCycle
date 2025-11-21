package com.sharecycle.ui;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.format.annotation.DateTimeFormat;

import com.sharecycle.application.BmsFacade;
import com.sharecycle.application.BmsFacade.TripCompletionResult;
import com.sharecycle.application.GetLastCompletedTripSummaryUseCase;
import com.sharecycle.application.GetTripDetailsUseCase;
import com.sharecycle.application.ListTripsUseCase;
import com.sharecycle.application.PaymentUseCase;
import com.sharecycle.domain.repository.JpaLedgerEntryRepository;
import com.sharecycle.domain.model.Bill;
import com.sharecycle.domain.model.LedgerEntry;
import com.sharecycle.domain.model.Trip;
import com.sharecycle.domain.model.User;
import com.sharecycle.domain.model.Bike;

@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final BmsFacade bmsFacade;
    private final GetTripDetailsUseCase getTripDetailsUseCase;
    private final ListTripsUseCase listTripsUseCase;
    private final GetLastCompletedTripSummaryUseCase getLastCompletedTripSummaryUseCase;
    private final JpaLedgerEntryRepository ledgerEntryRepository;
    private final PaymentUseCase paymentUseCase;

    public TripController(BmsFacade bmsFacade,
                          GetTripDetailsUseCase getTripDetailsUseCase,
                          ListTripsUseCase listTripsUseCase,
                          GetLastCompletedTripSummaryUseCase getLastCompletedTripSummaryUseCase,
                          JpaLedgerEntryRepository ledgerEntryRepository,
                          PaymentUseCase paymentUseCase) {
        this.bmsFacade = bmsFacade;
        this.getTripDetailsUseCase = getTripDetailsUseCase;
        this.listTripsUseCase = listTripsUseCase;
        this.getLastCompletedTripSummaryUseCase = getLastCompletedTripSummaryUseCase;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.paymentUseCase = paymentUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TripResponse startTrip(@RequestBody StartTripRequest request) {
        Trip trip = bmsFacade.startTrip(
                request.tripId(),
                request.riderId(),
                request.bikeId(),
                request.stationId(),
                request.startTime()
        );

        return new TripResponse(
                trip.getTripID(),
                trip.getStartStation().getId(),
                trip.getBike().getId(),
                trip.getRider().getUserId(),
                trip.getStartTime()
        );
    }

    @PostMapping("/{tripId}/end")
    public TripCompletionResponse endTrip(@PathVariable UUID tripId, @RequestBody EndTripRequest request) {
        TripCompletionResult result = bmsFacade.endTrip(tripId, request.stationId());
        if (result.isCompleted()) {
            Trip updatedTrip = result.trip();
            if (updatedTrip == null || result.ledgerEntry() == null) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Trip completion data missing.");
            }
            LedgerEntry ledgerEntry = result.ledgerEntry();
            Bill bill = ledgerEntry.getBill();
            LedgerEntry.LedgerStatus ledgerStatus = ledgerEntry.getLedgerStatus();
            String paymentStatus = derivePaymentStatus(ledgerStatus, bill != null ? bill.getTotalCost() : 0.0);
            double discountRate = 0.0;
            double discountAmount = 0.0;
            if (ledgerEntry.getTrip() != null) {
                discountRate = ledgerEntry.getTrip().getAppliedDiscountRate();
            }
            double preDiscount = bill != null ? bill.getBaseCost() + bill.getTimeCost() + bill.getEBikeSurcharge() : 0.0;
            discountAmount = bill != null ? Math.max(0.0, preDiscount - bill.getTotalCost()) : 0.0;
            return TripCompletionResponse.completed(
                    updatedTrip.getTripID(),
                    updatedTrip.getEndStation() != null ? updatedTrip.getEndStation().getId() : null,
                    updatedTrip.getEndTime(),
                    updatedTrip.getDurationMinutes(),
                    ledgerEntry.getLedgerId(),
                    bill != null ? bill.getBaseCost() : 0.0,
                    bill != null ? bill.getTimeCost() : 0.0,
                    bill != null ? bill.getEBikeSurcharge() : 0.0,
                    bill != null ? bill.getTotalCost() : 0.0,
                    ledgerStatus,
                    paymentStatus,
                    discountRate,
                    discountAmount
            );
        }

        if (result.isBlocked()) {
            var blockInfo = result.blockInfo();
            TripCompletionResponse.Credit credit = null;
            if (blockInfo != null && blockInfo.hasCredit()) {
                LedgerEntry creditLedger = blockInfo.creditLedgerEntry();
                Bill creditBill = creditLedger != null ? creditLedger.getBill() : null;
                double amount = creditBill != null ? Math.abs(creditBill.getTotalCost()) : 0.0;
                credit = new TripCompletionResponse.Credit(
                        creditLedger.getLedgerId(),
                        amount,
                        creditLedger.getDescription()
                );
            }
            List<TripCompletionResponse.StationSuggestion> suggestions = blockInfo != null
                    ? blockInfo.suggestions().stream()
                    .map(suggestion -> new TripCompletionResponse.StationSuggestion(
                            suggestion.stationId(),
                            suggestion.name(),
                            suggestion.freeDocks(),
                            suggestion.distanceMeters()
                    ))
                    .toList()
                    : List.of();
            UUID blockedStationId = blockInfo != null ? blockInfo.stationId() : request.stationId();
            String message = blockInfo != null ? blockInfo.message() : "Station was full.";
            return TripCompletionResponse.blocked(
                    tripId,
                    blockedStationId,
                    message,
                    credit,
                    suggestions
            );
        }

        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unhandled trip completion state.");
    }

    @GetMapping
    public List<ListTripsUseCase.TripHistoryEntry> listTrips(
            @RequestParam(name = "startTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(name = "endTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(name = "bikeType", required = false) String bikeTypeValue) {
        User currentUser = requireAuthenticatedUser();
        Bike.BikeType bikeType = null;
        if (bikeTypeValue != null && !bikeTypeValue.isBlank()) {
            try {
                bikeType = Bike.BikeType.valueOf(bikeTypeValue.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid bikeType value: " + bikeTypeValue);
            }
        }
        return listTripsUseCase.execute(currentUser, startTime, endTime, bikeType);
    }

    @GetMapping("/{tripId}")
    public GetTripDetailsUseCase.TripDetails getTripDetails(@PathVariable UUID tripId) {
        User currentUser = requireAuthenticatedUser();
        return getTripDetailsUseCase.execute(tripId, currentUser);
    }

    @GetMapping("/last-completed")
    public TripSummaryResponse getLastCompletedTrip() {
        User currentUser = requireAuthenticatedUser();
        if (!"RIDER".equalsIgnoreCase(currentUser.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Trip summary is available to riders only.");
        }
        GetLastCompletedTripSummaryUseCase.TripSummary summary =
                getLastCompletedTripSummaryUseCase.execute(currentUser.getUserId());
        if (summary == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No completed trips found.");
        }
        String paymentStatus = derivePaymentStatus(summary.ledgerStatus(), summary.totalCost());
        return TripSummaryResponse.from(summary, paymentStatus);
    }

    @PostMapping("/ledger/{ledgerId}/pay")
    public PaymentResult payLedger(@PathVariable UUID ledgerId) {
        User currentUser = requireAuthenticatedUser();
        LedgerEntry ledgerEntry = ledgerEntryRepository.findById(ledgerId);
        if (ledgerEntry == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ledger entry not found.");
        }
        if (!ownsLedger(currentUser, ledgerEntry) && !isOperator(currentUser)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot pay this ledger entry.");
        }
        LedgerEntry processed = paymentUseCase.execute(ledgerEntry);
        LedgerEntry finalEntry = processed != null ? processed : ledgerEntry;
        Bill bill = finalEntry.getBill();
        double total = bill != null ? bill.getTotalCost() : 0.0;
        String paymentStatus = derivePaymentStatus(finalEntry.getLedgerStatus(), total);
        return new PaymentResult(
                finalEntry.getLedgerId(),
                finalEntry.getLedgerStatus(),
                total,
                paymentStatus
        );
    }

    private User requireAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return user;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }

    private String derivePaymentStatus(LedgerEntry.LedgerStatus ledgerStatus, double totalCost) {
        if (totalCost <= 0) {
            return "NOT_REQUIRED";
        }
        if (ledgerStatus == LedgerEntry.LedgerStatus.PAID) {
            return "PAID";
        }
        return "PENDING";
    }

    private boolean ownsLedger(User requester, LedgerEntry ledgerEntry) {
        if (requester == null || ledgerEntry == null || ledgerEntry.getUser() == null) {
            return false;
        }
        UUID requesterId = requester.getUserId();
        return requesterId != null && requesterId.equals(ledgerEntry.getUser().getUserId());
    }

    private boolean isOperator(User requester) {
        if (requester == null || requester.getRole() == null) {
            return false;
        }
        String role = requester.getRole().toUpperCase();
        return "OPERATOR".equals(role) || "ADMIN".equals(role);
    }

    public record StartTripRequest(
            UUID tripId,
            UUID riderId,
            UUID bikeId,
            UUID stationId,
            LocalDateTime startTime
    ) { }

    public record TripResponse(
            UUID tripId,
            UUID stationId,
            UUID bikeId,
            UUID riderId,
            LocalDateTime startedAt
    ) { }

    public record EndTripRequest(UUID stationId) { }

    public record TripCompletionResponse(
            String status,
            UUID tripId,
            UUID stationId,
            LocalDateTime endedAt,
            Integer durationMinutes,
            UUID ledgerId,
            Double baseCost,
            Double timeCost,
            Double eBikeSurcharge,
            Double totalCost,
            LedgerEntry.LedgerStatus ledgerStatus,
            String paymentStatus,
            String message,
            Credit credit,
            List<StationSuggestion> suggestions,
            Double discountRate,
            Double discountAmount
    ) {
        public static TripCompletionResponse completed(UUID tripId,
                                                       UUID endStationId,
                                                       LocalDateTime endedAt,
                                                       Integer durationMinutes,
                                                       UUID ledgerId,
                                                       Double baseCost,
                                                       Double timeCost,
                                                       Double eBikeSurcharge,
                                                       Double totalCost,
                                                       LedgerEntry.LedgerStatus ledgerStatus,
                                                       String paymentStatus,
                                                       Double discountRate,
                                                       Double discountAmount) {
            return new TripCompletionResponse(
                    "COMPLETED",
                    tripId,
                    endStationId,
                    endedAt,
                    durationMinutes,
                    ledgerId,
                    baseCost,
                    timeCost,
                    eBikeSurcharge,
                    totalCost,
                    ledgerStatus,
                    paymentStatus,
                    "Trip completed successfully.",
                    null,
                    List.of(),
                    discountRate,
                    discountAmount
            );
        }

        public static TripCompletionResponse blocked(UUID tripId,
                                                     UUID stationId,
                                                     String message,
                                                     Credit credit,
                                                     List<StationSuggestion> suggestions) {
            return new TripCompletionResponse(
                    "BLOCKED",
                    tripId,
                    stationId,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    message,
                    credit,
                    suggestions,
                    null,
                    null
                                                
            );
        }

        public record Credit(UUID ledgerId, double amount, String description) { }

        public record StationSuggestion(UUID stationId, String name, int freeDocks, double distanceMeters) { }
    }

    public record TripSummaryResponse(
            UUID tripId,
            UUID endStationId,
            LocalDateTime endedAt,
            Integer durationMinutes,
            UUID ledgerId,
            double baseCost,
            double timeCost,
            double eBikeSurcharge,
            double totalCost,
            LedgerEntry.LedgerStatus ledgerStatus,
            String paymentStatus
    ) {
        static TripSummaryResponse from(GetLastCompletedTripSummaryUseCase.TripSummary summary,
                                        String paymentStatus) {
            return new TripSummaryResponse(
                    summary.tripId(),
                    summary.endStationId(),
                    summary.endedAt(),
                    summary.durationMinutes(),
                    summary.ledgerId(),
                    summary.baseCost(),
                    summary.timeCost(),
                    summary.eBikeSurcharge(),
                    summary.totalCost(),
                    summary.ledgerStatus(),
                    paymentStatus
            );
        }
    }

    public record PaymentResult(
            UUID ledgerId,
            LedgerEntry.LedgerStatus ledgerStatus,
            double totalCost,
            String paymentStatus
    ) { }
}
