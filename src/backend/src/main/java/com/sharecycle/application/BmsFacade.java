package com.sharecycle.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sharecycle.domain.model.Bike;
import com.sharecycle.domain.model.LedgerEntry;
import com.sharecycle.domain.model.Reservation;
import com.sharecycle.domain.model.Rider;
import com.sharecycle.domain.model.Station;
import com.sharecycle.domain.model.Trip;
import com.sharecycle.domain.model.User;
import com.sharecycle.domain.repository.JpaBikeRepository;
import com.sharecycle.domain.repository.JpaStationRepository;
import com.sharecycle.domain.repository.ReservationRepository;
import com.sharecycle.domain.repository.TripRepository;
import com.sharecycle.domain.repository.UserRepository;
import com.sharecycle.model.dto.StationDetailsDto;
import com.sharecycle.model.dto.StationSummaryDto;

@Service
public class BmsFacade {

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
                     ReservationRepository reservationRepository) {
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
    }

    @Transactional
    public com.sharecycle.domain.model.Reservation reserveBike(UUID riderId, UUID stationId, UUID bikeId, int expiresAfterMinutes) {
        User user = userRepository.findById(riderId);
        if (!(user instanceof Rider rider)) {
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
        if (!(user instanceof Rider rider)) {
            throw new IllegalStateException("User must be a rider to start a trip.");
        }
        Bike bike = requireBike(bikeId);
        Station station = requireStation(stationId);
        return startTripUseCase.execute(tripId, startTime != null ? startTime : LocalDateTime.now(), 0, rider, bike, station);
    }

    @Transactional
    public TripCompletionResult endTrip(UUID tripId, UUID stationId) {
        Trip trip = requireTrip(tripId);
        Station endStation = requireStation(stationId);
    LedgerEntry ledgerEntry = endTripAndBillUseCase.execute(trip, endStation);
        Trip updatedTrip = requireTrip(tripId);
        return new TripCompletionResult(updatedTrip, ledgerEntry);
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

    public record TripCompletionResult(Trip trip, LedgerEntry ledgerEntry) { }

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

