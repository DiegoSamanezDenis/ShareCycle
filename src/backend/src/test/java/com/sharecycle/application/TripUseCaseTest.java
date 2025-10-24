package com.sharecycle.application;

import com.sharecycle.domain.model.Bike;
import com.sharecycle.domain.model.Bill;
import com.sharecycle.domain.model.Dock;
import com.sharecycle.domain.model.LedgerEntry;
import com.sharecycle.domain.model.Rider;
import com.sharecycle.domain.model.Station;
import com.sharecycle.domain.model.Trip;
import com.sharecycle.domain.repository.JpaBikeRepository;
import com.sharecycle.domain.repository.JpaLedgerEntryRepository;
import com.sharecycle.domain.repository.JpaStationRepository;
import com.sharecycle.domain.repository.TripRepository;
import com.sharecycle.domain.repository.UserRepository;
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
    private TripRepository tripRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private JpaLedgerEntryRepository ledgerEntryRepository;
    
    @Autowired
    private JpaStationRepository stationRepository;

    @Autowired
    private JpaBikeRepository bikeRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

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
        stationRepository.save(startStation);

        Station managedStartStation = stationRepository.findById(startStation.getId());
        int startBikeDockedNumber = managedStartStation.getBikesDocked();
        Bike bikeForTrip = managedStartStation.getFirstDockWithBike().getOccupiedBike();

        // Assertion before trip start
        Trip trip = startTripUseCase.execute(
                UUID.randomUUID(),
                LocalDateTime.now(),
                0,
                rider,
                bikeForTrip,
                managedStartStation
        );

        Trip updatedTrip = tripRepository.findById(trip.getTripID());
        Station updatedStartStation = stationRepository.findById(updatedTrip.getStartStation().getId());
        Dock updatedStartDock = updatedStartStation.getDocks().getFirst();
        Bike updatedBike = bikeRepository.findById(updatedTrip.getBike().getId());

        // Assertion during trip
        assertThat(updatedStartStation.getBikesDocked()).isEqualTo(startBikeDockedNumber - 1);
        assertThat(updatedStartDock.getStatus()).isEqualTo(Dock.DockStatus.EMPTY);
        assertThat(updatedBike.getStatus()).isEqualTo(Bike.BikeStatus.ON_TRIP);

        Station endStation = new Station();
        endStation.setName("End Station");
        endStation.setLatitude(45.0);
        endStation.setLongitude(-73.0);
        endStation.setAddress("Address");
        endStation.markActive();
        endStation.addEmptyDocks(1);
        stationRepository.save(endStation);
        Station managedEndStation = stationRepository.findById(endStation.getId());
        int endBikeDockedNumber = managedEndStation.getBikesDocked();

        LedgerEntry ledgerEntry = endTripUseCase.execute(trip, managedEndStation);

        entityManager.flush();
        entityManager.clear();

        updatedTrip = tripRepository.findById(trip.getTripID()); // Should be the same tripId
        Station updatedEndStation = stationRepository.findById(updatedTrip.getEndStation().getId());
        Dock updatedEndDock = updatedEndStation.getDocks().getFirst();
        updatedBike = updatedTrip.getBike();

        //After trip assertions
        assertThat(updatedEndStation.getBikesDocked()).isEqualTo(endBikeDockedNumber + 1);
        assertThat(updatedEndDock.getStatus()).isEqualTo(Dock.DockStatus.OCCUPIED);
        assertThat(updatedBike.getStatus()).isEqualTo(Bike.BikeStatus.AVAILABLE);

        // Ledger
        LedgerEntry updatedLedgerEntry = ledgerEntryRepository.findById(ledgerEntry.getLedgerId());
        Bill bill = new Bill(updatedTrip);

        assertThat(updatedLedgerEntry.getTrip().getTripID()).isEqualTo(updatedTrip.getTripID());
        assertThat(ledgerEntry.getTotalAmount()).isEqualTo(bill.getTotal());

    }

    private Rider createRider() {
        return new Rider("Rider", "Rider","Rider", "Rider", "Rider", "Rider");
    }


}
