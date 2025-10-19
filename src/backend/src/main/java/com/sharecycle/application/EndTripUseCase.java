package com.sharecycle.application;

import com.sharecycle.domain.TripBuilder;
import com.sharecycle.domain.event.DomainEventPublisher;
import com.sharecycle.domain.event.StationStatusChangedEvent;
import com.sharecycle.domain.event.TripBilledEvent;
import com.sharecycle.domain.event.TripEndedEvent;
import com.sharecycle.infrastructure.*;
import com.sharecycle.model.entity.*;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class EndTripUseCase {

    private final Logger logger = LoggerFactory.getLogger(EndTripUseCase.class);

    private final DomainEventPublisher eventPublisher;

    private final JpaTripRepository jpaTripRepository;
    private final JpaLedgerEntryRepositoryImpl jpaLedgerEntryRepository;
    private final JpaStationRepositoryImpl jpaStationRepository;
    private final JpaBikeRepositoryImpl jpaBikeRepositoryImpl;


    public EndTripUseCase(DomainEventPublisher eventPublisher, JpaTripRepository jpaTripRepository,
                          JpaLedgerEntryRepositoryImpl jpaLedgerEntryRepository, JpaStationRepositoryImpl jpaStationRepository,
                          JpaBikeRepositoryImpl jpaBikeRepositoryImpl) {
        this.eventPublisher = eventPublisher;
        this.jpaTripRepository = jpaTripRepository;
        this.jpaLedgerEntryRepository = jpaLedgerEntryRepository;
        this.jpaStationRepository = jpaStationRepository;
        this.jpaBikeRepositoryImpl = jpaBikeRepositoryImpl;
    }
    @Transactional
    public LedgerEntry execute(Trip currentTrip, Station endStation){
        validate(currentTrip, endStation);

        LocalDateTime endTime = LocalDateTime.now();
        //Update the station and dock
        endStation.dockBike(currentTrip.getBike());
        jpaStationRepository.save(endStation);

        eventPublisher.publish(new StationStatusChangedEvent(
                endStation.getId(),
                endStation.getStatus(),
                endStation.getCapacity(),
                endStation.getBikesDocked()
        ));

        // Update the bike's status
        currentTrip.getBike().setStatus(Bike.BikeStatus.AVAILABLE);
        jpaBikeRepositoryImpl.save(currentTrip.getBike());

        // Update the trip: builder new Trip with same UUID to update the one in database
        TripBuilder tripBuilder = new TripBuilder(currentTrip);
        tripBuilder.endAt(endStation, endTime);
        Trip editedTrip = tripBuilder.build();
        jpaTripRepository.save(editedTrip);
        eventPublisher.publish(new TripEndedEvent(editedTrip.getTripID()));

        // Generate ledger
        LedgerEntry ledgerEntry = new LedgerEntry(editedTrip);
        jpaLedgerEntryRepository.save(ledgerEntry);
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
