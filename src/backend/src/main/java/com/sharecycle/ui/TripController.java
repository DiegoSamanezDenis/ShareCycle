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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.sharecycle.application.BmsFacade;
import com.sharecycle.application.BmsFacade.TripCompletionResult;
import com.sharecycle.application.GetTripDetailsUseCase;
import com.sharecycle.domain.model.Bill;
import com.sharecycle.domain.model.LedgerEntry;
import com.sharecycle.domain.model.Trip;
import com.sharecycle.domain.model.User;

@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final BmsFacade bmsFacade;
    private final GetTripDetailsUseCase getTripDetailsUseCase;

    public TripController(BmsFacade bmsFacade, GetTripDetailsUseCase getTripDetailsUseCase) {
        this.bmsFacade = bmsFacade;
        this.getTripDetailsUseCase = getTripDetailsUseCase;
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
            Bill bill = result.ledgerEntry().getBill();
            return TripCompletionResponse.completed(
                    updatedTrip.getTripID(),
                    updatedTrip.getEndStation() != null ? updatedTrip.getEndStation().getId() : null,
                    updatedTrip.getEndTime(),
                    updatedTrip.getDurationMinutes(),
                    result.ledgerEntry().getLedgerId(),
                    bill != null ? bill.getBaseCost() : 0.0,
                    bill != null ? bill.getTimeCost() : 0.0,
                    bill != null ? bill.getEBikeSurcharge() : 0.0,
                    bill != null ? bill.getTotalCost() : 0.0
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

    @GetMapping("/{tripId}")
    public GetTripDetailsUseCase.TripDetails getTripDetails(@PathVariable UUID tripId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = null;
        if (auth != null && auth.getPrincipal() instanceof User user) {
                currentUser = user;
        } else {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return getTripDetailsUseCase.execute(tripId, currentUser);
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
            String message,
            Credit credit,
            List<StationSuggestion> suggestions
    ) {
        public static TripCompletionResponse completed(UUID tripId,
                                                       UUID endStationId,
                                                       LocalDateTime endedAt,
                                                       Integer durationMinutes,
                                                       UUID ledgerId,
                                                       Double baseCost,
                                                       Double timeCost,
                                                       Double eBikeSurcharge,
                                                       Double totalCost) {
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
                    "Trip completed successfully.",
                    null,
                    List.of()
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
                    message,
                    credit,
                    suggestions
            );
        }

        public record Credit(UUID ledgerId, double amount, String description) { }

        public record StationSuggestion(UUID stationId, String name, int freeDocks, double distanceMeters) { }
    }
}
