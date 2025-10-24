package com.sharecycle.application;

import com.sharecycle.domain.TripBuilder;
import com.sharecycle.domain.event.DomainEventPublisher;
import com.sharecycle.domain.event.StationStatusChangedEvent;
import com.sharecycle.domain.event.TripBilledEvent;
import com.sharecycle.domain.event.TripEndedEvent;
import com.sharecycle.domain.model.Bike;
import com.sharecycle.domain.model.Dock;
import com.sharecycle.domain.model.LedgerEntry;
import com.sharecycle.domain.model.Reservation;
import com.sharecycle.domain.model.Station;
import com.sharecycle.domain.model.Trip;
import com.sharecycle.domain.repository.JpaBikeRepository;
import com.sharecycle.domain.repository.JpaLedgerEntryRepository;
import com.sharecycle.domain.repository.JpaStationRepository;
import com.sharecycle.domain.repository.JpaDockRepository;
import com.sharecycle.domain.repository.ReservationRepository;
import com.sharecycle.domain.repository.TripRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class EndTripUseCase {

    private final Logger logger = LoggerFactory.getLogger(EndTripUseCase.class);

    private final DomainEventPublisher eventPublisher;

    private final TripRepository tripRepository;
    private final JpaLedgerEntryRepository ledgerEntryRepository;
    private final JpaStationRepository stationRepository;
    private final JpaDockRepository dockRepository;
    private final JpaBikeRepository bikeRepository;
    private final ReservationRepository reservationRepository;


    @Autowired
    public EndTripUseCase(DomainEventPublisher eventPublisher,
                          TripRepository tripRepository,
                          JpaLedgerEntryRepository ledgerEntryRepository,
                          JpaStationRepository stationRepository,
                          JpaDockRepository dockRepository,
                          JpaBikeRepository bikeRepository,
                          ReservationRepository reservationRepository) {
        this.eventPublisher = eventPublisher;
        this.tripRepository = tripRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.stationRepository = stationRepository;
        this.dockRepository = dockRepository;
        this.bikeRepository = bikeRepository;
        this.reservationRepository = reservationRepository;
    }

    // Backward-compat constructor for existing tests
    public EndTripUseCase(DomainEventPublisher eventPublisher,
                          TripRepository tripRepository,
                          JpaLedgerEntryRepository ledgerEntryRepository,
                          JpaStationRepository stationRepository,
                          JpaBikeRepository bikeRepository,
                          ReservationRepository reservationRepository) {
        this(
                eventPublisher,
                tripRepository,
                ledgerEntryRepository,
                stationRepository,
                new JpaDockRepository() {
                    @Override
                    public void save(Dock dock) { /* no-op for tests */ }

                    @Override
                    public Dock findById(UUID id) { return null; }

                    @Override
                    public List<Dock> findAll() { return List.of(); }

                    @Override
                    public int clearBikeFromAllDocks(UUID bikeId) { return 0; }
                },
                bikeRepository,
                reservationRepository
        );
    }
    @Transactional
    public LedgerEntry execute(Trip currentTrip, Station endStation){
        Trip managedTrip = tripRepository.findById(currentTrip.getTripID());
        if (managedTrip == null) {
            throw new IllegalArgumentException("Trip not found.");
        }
        if (managedTrip.getEndTime() != null) {
            throw new IllegalStateException("Trip already completed.");
        }

        Station managedEndStation = stationRepository.findByIdForUpdate(endStation.getId());
        if (managedEndStation == null) {
            throw new IllegalArgumentException("Destination station not found.");
        }

        Bike tripBike = managedTrip.getBike();
        if (tripBike == null) {
            throw new IllegalStateException("Bike not found for trip.");
        }

        validate(managedTrip, managedEndStation);

        // Reconcile bike status if needed to ensure trip can end cleanly
        if (tripBike.getStatus() != Bike.BikeStatus.ON_TRIP) {
            logger.warn("Bike status {} inconsistent with active trip; reconciling to ON_TRIP", tripBike.getStatus());
            tripBike.setStatus(Bike.BikeStatus.ON_TRIP);
        }

        LocalDateTime endTime = LocalDateTime.now();

        // Defensive: clear any remaining dock assignment for this bike across the system
        dockRepository.clearBikeFromAllDocks(tripBike.getId());

        // Update the station and dock
        managedEndStation.dockBike(tripBike);
        stationRepository.save(managedEndStation);

        eventPublisher.publish(new StationStatusChangedEvent(
                managedEndStation.getId(),
                managedEndStation.getStatus(),
                managedEndStation.getCapacity(),
                managedEndStation.getBikesDocked()
        ));

        // Update the bike's status now that it is docked
        tripBike.completeTrip();
        tripBike.setStatus(Bike.BikeStatus.AVAILABLE);
        tripBike.setReservationExpiry(null);
        tripBike.setCurrentStation(managedEndStation);
        bikeRepository.save(tripBike);

        Reservation activeReservation = reservationRepository.findByRiderId(managedTrip.getRider().getUserId());
        if (activeReservation != null) {
            activeReservation.expire();
            reservationRepository.save(activeReservation);
        }

        // Update the trip
        TripBuilder tripBuilder = new TripBuilder(managedTrip);
        tripBuilder.endAt(managedEndStation, endTime);
        Trip editedTrip = tripBuilder.build();
        tripRepository.save(editedTrip);
        eventPublisher.publish(new TripEndedEvent(editedTrip.getTripID()));

        // Generate ledger
        LedgerEntry ledgerEntry = new LedgerEntry(editedTrip);
        ledgerEntryRepository.save(ledgerEntry);
        eventPublisher.publish(new TripBilledEvent(editedTrip.getTripID(), ledgerEntry.getLedgerId()));

        return ledgerEntry;
    }


    // Simple validation
    private void validate(Trip currentTrip, Station station){
        if (station.isOutOfService()) {
            logger.error("Destination station is out of service");
            throw new IllegalStateException("Destination station is out of service");
        }
        if (!station.hasFreeDock()){
            logger.error("Unexpected error: Station is full");
            throw new IllegalStateException("Unexpected error: Station is full");
        }
    }
}
