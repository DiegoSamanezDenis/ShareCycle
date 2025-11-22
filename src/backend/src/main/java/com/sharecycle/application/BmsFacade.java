package com.sharecycle.application;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sharecycle.domain.model.Bike;
import com.sharecycle.domain.model.Bill;
import com.sharecycle.domain.model.LedgerEntry;
import com.sharecycle.domain.model.Operator;
import com.sharecycle.domain.model.Reservation;
import com.sharecycle.domain.model.Rider;
import com.sharecycle.domain.model.Station;
import com.sharecycle.domain.model.Trip;
import com.sharecycle.domain.model.User;
import com.sharecycle.domain.repository.JpaBikeRepository;
import com.sharecycle.domain.repository.JpaLedgerEntryRepository;
import com.sharecycle.domain.repository.JpaStationRepository;
import com.sharecycle.domain.repository.ReservationRepository;
import com.sharecycle.domain.repository.TripRepository;
import com.sharecycle.domain.repository.UserRepository;
import com.sharecycle.model.dto.StationDetailsDto;
import com.sharecycle.model.dto.StationSummaryDto;

@Service
public class BmsFacade {

    private static final Logger logger = LoggerFactory.getLogger(BmsFacade.class);
    private static final double FULL_STATION_COURTESY_CREDIT = 1.00d;
    private static final String FULL_STATION_CREDIT_DESCRIPTION = "Credit issued because the destination station was full.";
    private static final double EARTH_RADIUS_METERS = 6_371_000d;

    private final ReserveBikeUseCase reserveBikeUseCase;
    private final StartTripUseCase startTripUseCase;
    private final EndTripAndBillUseCase endTripAndBillUseCase;
    private final MoveBikeUseCase moveBikeUseCase;
    private final SetStationStatusUseCase setStationStatusUseCase;
    private final AdjustStationCapacityUseCase adjustStationCapacityUseCase;
    private final ListStationSummariesUseCase listStationSummariesUseCase;

    private final UserRepository userRepository;
    private final JpaStationRepository stationRepository;
    private final JpaBikeRepository bikeRepository;
    private final TripRepository tripRepository;
    private final ReservationRepository reservationRepository;
    private final JpaLedgerEntryRepository ledgerEntryRepository;

    public BmsFacade(ReserveBikeUseCase reserveBikeUseCase,
                     StartTripUseCase startTripUseCase,
                     EndTripAndBillUseCase endTripAndBillUseCase,
                     MoveBikeUseCase moveBikeUseCase,
                     SetStationStatusUseCase setStationStatusUseCase,
                     AdjustStationCapacityUseCase adjustStationCapacityUseCase,
                     ListStationSummariesUseCase listStationSummariesUseCase,
                     UserRepository userRepository,
                     JpaStationRepository stationRepository,
                     JpaBikeRepository bikeRepository,
                     TripRepository tripRepository,
                     ReservationRepository reservationRepository,
                     JpaLedgerEntryRepository ledgerEntryRepository) {
        this.reserveBikeUseCase = reserveBikeUseCase;
        this.startTripUseCase = startTripUseCase;
        this.endTripAndBillUseCase = endTripAndBillUseCase;
        this.moveBikeUseCase = moveBikeUseCase;
        this.setStationStatusUseCase = setStationStatusUseCase;
        this.adjustStationCapacityUseCase = adjustStationCapacityUseCase;
        this.listStationSummariesUseCase = listStationSummariesUseCase;
        this.userRepository = userRepository;
        this.stationRepository = stationRepository;
        this.bikeRepository = bikeRepository;
        this.tripRepository = tripRepository;
        this.reservationRepository = reservationRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Transactional
    public com.sharecycle.domain.model.Reservation reserveBike(UUID riderId, UUID stationId, UUID bikeId, int expiresAfterMinutes) {
        User user = userRepository.findById(riderId);
        Rider rider;
        if (user instanceof Rider r) {
            rider = r;
        } else if (user instanceof Operator) {
            // Check if operator has ROLE_RIDER authority (set by SessionAuthenticationFilter when in RIDER mode)
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean hasRiderRole = auth != null && auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_RIDER"));
            if (hasRiderRole) {
                // Convert operator in RIDER mode to Rider
                rider = new Rider(user);
            } else {
                throw new IllegalStateException("User must be a rider to reserve bikes.");
            }
        } else {
            throw new IllegalStateException("User must be a rider to reserve bikes.");
        }
        Station station = requireStation(stationId);
        Bike bike = requireBike(bikeId);
        return reserveBikeUseCase.execute(rider, station, bike, expiresAfterMinutes);
    }

    @Transactional
    public Trip startTrip(UUID tripId,
                          UUID riderId,
                          UUID bikeId,
                          UUID stationId,
                          LocalDateTime startTime) {
        User user = userRepository.findById(riderId);
        logger.info("startTrip - User type: {}, User role: {}", user.getClass().getSimpleName(), user.getRole());
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            logger.info("startTrip - Authentication principal: {}", auth.getPrincipal());
            logger.info("startTrip - Authorities: {}", auth.getAuthorities());
        } else {
            logger.warn("startTrip - No authentication in SecurityContext");
        }
        
        // Validate effective role
        if (user instanceof Rider) {
            // Already a rider, good to go
        } else if (user instanceof Operator) {
            // Check if operator has ROLE_RIDER authority (set by SessionAuthenticationFilter when in RIDER mode)
            boolean hasRiderRole = auth != null && auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_RIDER"));
            logger.info("startTrip - Operator hasRiderRole: {}", hasRiderRole);
            if (!hasRiderRole) {
                throw new IllegalStateException("User must be a rider to start a trip.");
            }
        } else {
            throw new IllegalStateException("User must be a rider to start a trip.");
        }
        
        Bike bike = requireBike(bikeId);
        Station station = requireStation(stationId);
        return startTripUseCase.execute(tripId, startTime != null ? startTime : LocalDateTime.now(), 0, user, bike, station);
    }

    @Transactional(noRollbackFor = StationFullException.class)
    public TripCompletionResult endTrip(UUID tripId, UUID stationId) {
        Trip trip = requireTrip(tripId);
        Station endStation = requireStation(stationId);
        try {
            LedgerEntry ledgerEntry = endTripAndBillUseCase.execute(trip, endStation);
            Trip updatedTrip = requireTrip(tripId);
            return TripCompletionResult.completed(updatedTrip, ledgerEntry);
        } catch (StationFullException fullException) {
            Station freshestView = stationRepository.findById(fullException.getStationId());
            Station blockedStation = freshestView != null ? freshestView : endStation;
            ReturnBlockInfo blockInfo = resolveBlockedReturn(trip, blockedStation);
            return TripCompletionResult.blocked(blockInfo);
        }
    }

    private ReturnBlockInfo resolveBlockedReturn(Trip trip, Station blockedStation) {
        List<TripCompletionResult.StationSuggestion> suggestions = stationRepository.findAll().stream()
                .filter(candidate -> !candidate.getId().equals(blockedStation.getId()))
                .filter(candidate -> !candidate.isOutOfService())
                .filter(Station::hasFreeDock)
                .map(candidate -> new TripCompletionResult.StationSuggestion(
                        candidate.getId(),
                        candidate.getName(),
                        candidate.getFreeDockCount(),
                        Math.round(distanceMeters(blockedStation, candidate))
                ))
                .sorted(Comparator.comparingDouble(TripCompletionResult.StationSuggestion::distanceMeters))
                .limit(3)
                .toList();

        if (!suggestions.isEmpty()) {
            String message = "Station is full. Try one of the nearby stations.";
            return new ReturnBlockInfo(blockedStation.getId(), message, suggestions, null);
        }

        LedgerEntry existingCredit = findExistingCourtesyCredit(trip);
        if (existingCredit != null) {
            String message = "Station is still full. Courtesy credit already applied.";
            return new ReturnBlockInfo(blockedStation.getId(), message, List.of(), existingCredit);
        }

        LedgerEntry creditLedgerEntry = createCourtesyCredit(trip, blockedStation);
        ledgerEntryRepository.save(creditLedgerEntry);
        String message = "Station is full. A courtesy credit has been applied to your account.";
        return new ReturnBlockInfo(blockedStation.getId(), message, List.of(), creditLedgerEntry);
    }

    private LedgerEntry createCourtesyCredit(Trip trip, Station blockedStation) {
        Bill creditBill = new Bill(
                null,
                LocalDateTime.now(),
                0.0,
                0.0,
                0.0,
                -FULL_STATION_COURTESY_CREDIT
        );

        String stationLabel = blockedStation.getName() != null ? blockedStation.getName() : "the station";
        String description = String.format(
                "%s (Trip %s near %s)",
                FULL_STATION_CREDIT_DESCRIPTION,
                trip.getTripID(),
                stationLabel
        );

        return new LedgerEntry(
                trip.getRider(),
                null,
                creditBill,
                "RETURN_BLOCK_CREDIT",
                description
        );
    }

    private LedgerEntry findExistingCourtesyCredit(Trip trip) {
        List<LedgerEntry> entries = ledgerEntryRepository.findAllByUser(trip.getRider());
        if (entries == null || entries.isEmpty()) {
            return null;
        }
        return entries.stream()
                .filter(entry -> "RETURN_BLOCK_CREDIT".equals(entry.getPricingPlan()))
                .filter(entry -> entry.getDescription() != null && entry.getDescription().contains(trip.getTripID().toString()))
                .findFirst()
                .orElse(null);
    }

    private double distanceMeters(Station origin, Station candidate) {
        double originLat = Math.toRadians(origin.getLatitude());
        double originLon = Math.toRadians(origin.getLongitude());
        double candidateLat = Math.toRadians(candidate.getLatitude());
        double candidateLon = Math.toRadians(candidate.getLongitude());

        double deltaLat = candidateLat - originLat;
        double deltaLon = candidateLon - originLon;

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(originLat) * Math.cos(candidateLat)
                * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }

    @Transactional
    public void moveBike(UUID operatorId, UUID bikeId, UUID destinationStationId) {
        moveBikeUseCase.execute(new MoveBikeUseCase.Command(operatorId, bikeId, destinationStationId));
    }

    @Transactional
    public Station updateStationStatus(UUID operatorId, UUID stationId, boolean outOfService) {
        return setStationStatusUseCase.execute(new SetStationStatusUseCase.Command(operatorId, stationId, outOfService));
    }

    @Transactional
    public Station adjustStationCapacity(UUID operatorId, UUID stationId, int delta) {
        return adjustStationCapacityUseCase.execute(new AdjustStationCapacityUseCase.Command(operatorId, stationId, delta));
    }

    @Transactional(readOnly = true)
    public List<StationSummaryDto> listStations() {
        return listStationSummariesUseCase.execute();
    }

    @Transactional(readOnly = true)
    public Station findStation(UUID stationId) {
        return requireStation(stationId);
    }

    @Transactional(readOnly = true)
    public StationDetailsDto getStationDetails(UUID stationId, UUID principalUserId) {
        Station station = requireStation(stationId);
        return toStationDetailsDto(station, principalUserId);
    }

    @Transactional(readOnly = true)
    public Reservation getActiveReservation(UUID riderId) {
        return reservationRepository.findByRiderId(riderId);
    }

    private Station requireStation(UUID stationId) {
        Station station = stationRepository.findById(stationId);
        if (station == null) {
            throw new IllegalArgumentException("Station not found.");
        }
        return station;
    }

    private Bike requireBike(UUID bikeId) {
        Bike bike = bikeRepository.findById(bikeId);
        if (bike == null) {
            throw new IllegalArgumentException("Bike not found.");
        }
        return bike;
    }

    private Trip requireTrip(UUID tripId) {
        Trip trip = tripRepository.findById(tripId);
        if (trip == null) {
            throw new IllegalArgumentException("Trip not found.");
        }
        return trip;
    }

    public static class TripCompletionResult {
        public enum Status { COMPLETED, BLOCKED }

        private final Status status;
        private final Trip trip;
        private final LedgerEntry ledgerEntry;
        private final ReturnBlockInfo blockInfo;

        private TripCompletionResult(Status status, Trip trip, LedgerEntry ledgerEntry, ReturnBlockInfo blockInfo) {
            this.status = status;
            this.trip = trip;
            this.ledgerEntry = ledgerEntry;
            this.blockInfo = blockInfo;
        }

        public static TripCompletionResult completed(Trip trip, LedgerEntry ledgerEntry) {
            return new TripCompletionResult(Status.COMPLETED, trip, ledgerEntry, null);
        }

        public static TripCompletionResult blocked(ReturnBlockInfo blockInfo) {
            return new TripCompletionResult(Status.BLOCKED, null, null, blockInfo);
        }

        public Status status() {
            return status;
        }

        public Trip trip() {
            return trip;
        }

        public LedgerEntry ledgerEntry() {
            return ledgerEntry;
        }

        public ReturnBlockInfo blockInfo() {
            return blockInfo;
        }

        public boolean isCompleted() {
            return status == Status.COMPLETED;
        }

        public boolean isBlocked() {
            return status == Status.BLOCKED;
        }

        public record StationSuggestion(
                UUID stationId,
                String name,
                int freeDocks,
                double distanceMeters
        ) { }
    }

    public static class ReturnBlockInfo {
        private final UUID stationId;
        private final String message;
        private final List<TripCompletionResult.StationSuggestion> suggestions;
        private final LedgerEntry creditLedgerEntry;

        public ReturnBlockInfo(UUID stationId,
                               String message,
                               List<TripCompletionResult.StationSuggestion> suggestions,
                               LedgerEntry creditLedgerEntry) {
            this.stationId = stationId;
            this.message = message;
            this.suggestions = suggestions;
            this.creditLedgerEntry = creditLedgerEntry;
        }

        public UUID stationId() {
            return stationId;
        }

        public String message() {
            return message;
        }

        public List<TripCompletionResult.StationSuggestion> suggestions() {
            return suggestions;
        }

        public LedgerEntry creditLedgerEntry() {
            return creditLedgerEntry;
        }

        public boolean hasCredit() {
            return creditLedgerEntry != null;
        }
    }

    private StationDetailsDto toStationDetailsDto(Station station, UUID principalUserId) {
        List<StationDetailsDto.DockDto> docks = station.getDocks().stream()
                .map(dock -> new StationDetailsDto.DockDto(
                        dock.getId(),
                        dock.getStatus(),
                        dock.getOccupiedBike() != null ? dock.getOccupiedBike().getId() : null
                ))
                .toList();
        boolean isOperator = false;
        boolean isRider = false;
        if (principalUserId != null) {
            User user = userRepository.findById(principalUserId);
            if (user != null && user.getRole() != null) {
                isOperator = "OPERATOR".equalsIgnoreCase(user.getRole()) || "ADMIN".equalsIgnoreCase(user.getRole());
                isRider = "RIDER".equalsIgnoreCase(user.getRole());
            }
        }
        boolean stationActive = !station.isOutOfService();
        boolean hasAvailableBike = station.getAvailableBikeCount() > 0;
        boolean hasFreeDock = station.getFreeDockCount() > 0;
        boolean canReserve = stationActive && hasAvailableBike;
        boolean canStartTrip = stationActive && hasAvailableBike;
        boolean canReturn = false;
        boolean canMove = isOperator && stationActive;
        boolean canToggleStatus = isOperator;

        if(isRider && principalUserId != null) {
            boolean riderHasActiveTrip = tripRepository.riderHasActiveTrip(principalUserId);
            Reservation riderReservation = reservationRepository.findByRiderId(principalUserId);

            if (riderHasActiveTrip) {
                canReserve = false;
                canStartTrip = false;
                canReturn = hasFreeDock && stationActive;
            } else if (riderReservation != null && riderReservation.isActive()) {
                canReserve = false;
                UUID reserveStationId = riderReservation.getStation() != null ? riderReservation.getStation().getId() : null;
                canStartTrip = stationActive && reserveStationId != null && reserveStationId.equals(station.getId());
                canReturn = false;
            } else {
                canReserve = stationActive && hasAvailableBike;
                canStartTrip = stationActive && hasAvailableBike;
                canReturn = false;
            }
        }
        return new StationDetailsDto(
                station.getId(),
                station.getName(),
                station.getStatus(),
                station.getCapacity(),
                station.getBikesDocked(),
                station.getFreeDockCount(),
                docks,
                canReserve,
                canStartTrip,
                canReturn,
                canMove,
                canToggleStatus
        );
    }
}

