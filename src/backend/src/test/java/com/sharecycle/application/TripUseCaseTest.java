package com.sharecycle.application;

import com.sharecycle.infrastructure.*;
import com.sharecycle.model.entity.*;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class TripUseCaseTest {

    @Autowired
    private StartTripUseCase startTripUseCase;

    @Autowired
    private EndTripUseCase endTripUseCase;
    
    @Autowired
    private JpaTripRepository jpaTripRepository;
    
    @Autowired
    private JpaUserRepository userRepository;
    
    @Autowired
    private JpaLedgerEntryRepositoryImpl jpaLedgerEntryRepository;
    
    @Autowired
    private JpaStationRepositoryImpl jpaStationRepository;
    
    @Autowired
    private JpaDockRepositoryImpl jpaDockRepository;

    @Autowired
    private JpaBikeRepositoryImpl jpaBikeRepositoryImpl;

    @Test
    @Transactional
    void test() {
        Rider rider = createRider();
        rider.setRole("RIDER");
        userRepository.save(rider);

        Station startStation = new Station();
        startStation.setName("Start Station");
        startStation.setLatitude(45.0);
        startStation.setLongitude(-73.0);
        startStation.setAddress("Address");
        startStation.markActive();
        startStation.addEmptyDocks(1);
        startStation.getDocks().getFirst().setOccupiedBike(new Bike(Bike.BikeType.STANDARD));
        jpaStationRepository.save(startStation);

        int startBikeDockedNumber = startStation.getBikesDocked();
        // Assertion before trip start
        Trip trip = startTripUseCase.execute(
                UUID.randomUUID(),
                LocalDateTime.now(),
                0,
                rider,
                startStation.getFirstDockWithBike().getOccupiedBike(),
                startStation
        );

        Trip updatedTrip = jpaTripRepository.findById(trip.getTripID());
        Station updatedStartStation = jpaStationRepository.findById(updatedTrip.getStartStation().getId());
        Dock updatedStartDock = updatedStartStation.getDocks().getFirst();
        Bike updatedBike = jpaBikeRepositoryImpl.findById(updatedTrip.getBike().getId());

        // Assertion during trip
        assertThat(startBikeDockedNumber == (updatedStartStation.getBikesDocked() + 1));
        assertThat(updatedStartDock.getStatus() == Dock.DockStatus.EMPTY);
        assertThat(updatedBike.getStatus() == Bike.BikeStatus.ON_TRIP);

        Station endStation = new Station();
        endStation.setName("End Station");
        endStation.setLatitude(45.0);
        endStation.setLongitude(-73.0);
        endStation.setAddress("Address");
        endStation.markActive();
        endStation.addEmptyDocks(1);
        jpaStationRepository.save(endStation);
        int endBikeDockedNumber = startStation.getBikesDocked();

        LedgerEntry ledgerEntry = endTripUseCase.execute(trip, endStation);

        updatedTrip = jpaTripRepository.findById(trip.getTripID()); // Should be the same tripId
        Station updatedEndStation = jpaStationRepository.findById(updatedTrip.getEndStation().getId());
        Dock updatedEndDock = updatedEndStation.getDocks().getFirst();
        updatedBike = jpaBikeRepositoryImpl.findById(updatedTrip.getBike().getId());

        //After trip assertions
        assertThat(endBikeDockedNumber == (updatedEndStation.getBikesDocked() - 1));
        assertThat(updatedEndDock.getStatus() == Dock.DockStatus.OCCUPIED);
        assertThat(updatedBike.getStatus() == Bike.BikeStatus.AVAILABLE);

        // Ledger
        LedgerEntry updatedLedgerEntry = jpaLedgerEntryRepository.findById(ledgerEntry.getLedgerId());
        Bill bill = new Bill(updatedTrip);

        assertThat(updatedLedgerEntry.getTrip().getTripID() == updatedTrip.getTripID());
        assertThat(ledgerEntry.getTotalAmount() == bill.getTotal());

    }

    private Rider createRider() {
        return new Rider("Rider", "Rider","Rider", "Rider", "Rider", "Rider");
    }


}
