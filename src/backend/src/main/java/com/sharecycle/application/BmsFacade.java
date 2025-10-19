package com.sharecycle.application;

import com.sharecycle.domain.model.Bike;
import com.sharecycle.domain.model.LedgerEntry;
import com.sharecycle.domain.model.Rider;
import com.sharecycle.domain.model.Station;
import com.sharecycle.domain.model.Trip;
import com.sharecycle.domain.model.User;
import com.sharecycle.domain.repository.JpaBikeRepository;
import com.sharecycle.domain.repository.JpaStationRepository;
import com.sharecycle.domain.repository.ReservationRepository;
import com.sharecycle.domain.repository.TripRepository;
import com.sharecycle.domain.repository.UserRepository;
import com.sharecycle.model.dto.StationSummaryDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BmsFacade {

    private final ReserveBikeUseCase reserveBikeUseCase;
    private final StartTripUseCase startTripUseCase;
    private final EndTripUseCase endTripUseCase;
    private final MoveBikeUseCase moveBikeUseCase;
    private final SetStationStatusUseCase setStationStatusUseCase;
    private final AdjustStationCapacityUseCase adjustStationCapacityUseCase;
    private final ListStationSummariesUseCase listStationSummariesUseCase;

    private final UserRepository userRepository;
    private final JpaStationRepository stationRepository;
    private final JpaBikeRepository bikeRepository;
    private final TripRepository tripRepository;

    public BmsFacade(ReserveBikeUseCase reserveBikeUseCase,
                     StartTripUseCase startTripUseCase,
                     EndTripUseCase endTripUseCase,
                     MoveBikeUseCase moveBikeUseCase,
                     SetStationStatusUseCase setStationStatusUseCase,
                     AdjustStationCapacityUseCase adjustStationCapacityUseCase,
                     ListStationSummariesUseCase listStationSummariesUseCase,
                     UserRepository userRepository,
                     JpaStationRepository stationRepository,
                     JpaBikeRepository bikeRepository,
                     TripRepository tripRepository) {
        this.reserveBikeUseCase = reserveBikeUseCase;
        this.startTripUseCase = startTripUseCase;
        this.endTripUseCase = endTripUseCase;
        this.moveBikeUseCase = moveBikeUseCase;
        this.setStationStatusUseCase = setStationStatusUseCase;
        this.adjustStationCapacityUseCase = adjustStationCapacityUseCase;
        this.listStationSummariesUseCase = listStationSummariesUseCase;
        this.userRepository = userRepository;
        this.stationRepository = stationRepository;
        this.bikeRepository = bikeRepository;
        this.tripRepository = tripRepository;
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
        LedgerEntry ledgerEntry = endTripUseCase.execute(trip, endStation);
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
}
