package com.sharecycle.application;

import com.sharecycle.domain.TripBuilder;
import com.sharecycle.domain.event.DomainEventPublisher;
import com.sharecycle.domain.event.StationStatusChangedEvent;
import com.sharecycle.domain.event.TripBilledEvent;
import com.sharecycle.domain.event.TripEndedEvent;
import com.sharecycle.domain.model.Bike;
import com.sharecycle.domain.model.LedgerEntry;
import com.sharecycle.domain.model.Station;
import com.sharecycle.domain.model.Trip;
import com.sharecycle.domain.repository.JpaBikeRepository;
import com.sharecycle.domain.repository.JpaLedgerEntryRepository;
import com.sharecycle.domain.repository.JpaStationRepository;
import com.sharecycle.domain.repository.TripRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class EndTripUseCase {

    private final Logger logger = LoggerFactory.getLogger(EndTripUseCase.class);

    private final DomainEventPublisher eventPublisher;

    private final TripRepository tripRepository;
    private final JpaLedgerEntryRepository ledgerEntryRepository;
    private final JpaStationRepository stationRepository;
    private final JpaBikeRepository bikeRepository;


    public EndTripUseCase(DomainEventPublisher eventPublisher,
                          TripRepository tripRepository,
                          JpaLedgerEntryRepository ledgerEntryRepository,
                          JpaStationRepository stationRepository,
                          JpaBikeRepository bikeRepository) {
        this.eventPublisher = eventPublisher;
        this.tripRepository = tripRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.stationRepository = stationRepository;
        this.bikeRepository = bikeRepository;
    }
    @Transactional
    public LedgerEntry execute(Trip currentTrip, Station endStation){
        validate(currentTrip, endStation);

        LocalDateTime endTime = LocalDateTime.now();
        //Update the station and dock
        endStation.dockBike(currentTrip.getBike());
        stationRepository.save(endStation);

        eventPublisher.publish(new StationStatusChangedEvent(
                endStation.getId(),
                endStation.getStatus(),
                endStation.getCapacity(),
                endStation.getBikesDocked()
        ));

        // Update the bike's status
        currentTrip.getBike().setStatus(Bike.BikeStatus.AVAILABLE);
        bikeRepository.save(currentTrip.getBike());

        // Update the trip: builder new Trip with same UUID to update the one in database
        TripBuilder tripBuilder = new TripBuilder(currentTrip);
        tripBuilder.endAt(endStation, endTime);
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
        // Validate non-null inputs
        if (currentTrip == null || station == null){
            logger.error("Unexpected error: Trip or station is null");
            throw new RuntimeException("Unexpected error: Trip or Dock is null");
        }
        // Validate the station has a free dock
        if (!station.hasFreeDock()){
            logger.error("Unexpected error: Station is full");
            throw new RuntimeException("Unexpected error: Station is full");
        }
    }
}
