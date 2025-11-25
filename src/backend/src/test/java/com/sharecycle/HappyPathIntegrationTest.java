package com.sharecycle;

import com.sharecycle.application.BmsFacade;
import com.sharecycle.domain.model.*;
import com.sharecycle.domain.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class HappyPathIntegrationTest {

    @Autowired private BmsFacade bmsFacade;
    @Autowired private UserRepository userRepository;
    @Autowired private JpaStationRepository stationRepository;
    @Autowired private JpaBikeRepository bikeRepository;
    @Autowired private TripRepository tripRepository;

    @Test
    @DisplayName("Happy Path: Reserve -> Start (30 min ago) -> End -> Bill")
    void testCompleteRideWorkflow() {
        // 1. SETUP: Create Rider, Stations, and Bike
        Rider rider = new Rider(
                "Happy Rider", "123 Test Lane", "happy@test.com",
                "happy_rider", "password", "pm_visa_card",
                PricingPlan.PlanType.PAY_AS_YOU_GO
        );
        userRepository.save(rider);

        Station stationA = new Station(UUID.randomUUID(), "Station A", Station.StationStatus.EMPTY, 45.5, -73.5, "Address A", 5, 0);
        stationA.addEmptyDocks(5);
        stationRepository.save(stationA);

        Station stationB = new Station(UUID.randomUUID(), "Station B", Station.StationStatus.EMPTY, 45.6, -73.6, "Address B", 5, 0);
        stationB.addEmptyDocks(5);
        stationRepository.save(stationB);

        Bike bike = new Bike(Bike.BikeType.STANDARD);
        bike.setStatus(Bike.BikeStatus.AVAILABLE);
        bikeRepository.save(bike);

        // Dock bike at Station A
        stationA.dockBike(bike);
        stationRepository.save(stationA);

        System.out.println("--- SETUP COMPLETE ---");

        // 2. RESERVE: Rider reserves bike at Station A
        Reservation reservation = bmsFacade.reserveBike(rider.getUserId(), stationA.getId(), bike.getId(), 15);

        assertNotNull(reservation, "Reservation should be created");
        assertEquals(bike.getId(), reservation.getBike().getId());
        assertTrue(reservation.isActive(), "Reservation should be active");

        System.out.println("--- RESERVATION SUCCESSFUL ---");

        // 3. START TRIP: Unlock bike (Simulate start time 30 mins ago)
        LocalDateTime startTime = LocalDateTime.now().minusMinutes(30);

        Trip trip = bmsFacade.startTrip(
                UUID.randomUUID(),
                rider.getUserId(),
                bike.getId(),
                stationA.getId(),
                startTime
        );

        assertNotNull(trip, "Trip should be started");

        Bike tripBike = bikeRepository.findById(bike.getId());
        System.out.println(tripBike.getStatus());
        assertEquals(Bike.BikeStatus.ON_TRIP, tripBike.getStatus());

        System.out.println("--- TRIP STARTED ---");

        // 4. END TRIP: Return bike at Station B
        BmsFacade.TripCompletionResult result = bmsFacade.endTrip(trip.getTripID(), stationB.getId());

        LedgerEntry ledger = result.ledgerEntry();

        // Verify completion
        assertEquals(stationB.getId(), result.trip().getEndStation().getId());

        Bike returnedBike = bikeRepository.findById(bike.getId());
        assertEquals(Bike.BikeStatus.AVAILABLE, returnedBike.getStatus());

        System.out.println("--- TRIP ENDED ---");

        // 5. BILLING VALIDATION
        long duration = result.trip().getDurationMinutes();
        assertEquals(30, duration, "Duration should be 30 minutes");

        double expectedTotal= 30 * 6.0;

        Bill bill = ledger.getBill();
        assertNotNull(bill, "Bill object should generate successfully");

        System.out.printf("Bill Total: $%.2f (Expected ~ $%.2f)%n", bill.getTotalCost(), expectedTotal);

        assertEquals(expectedTotal, bill.getTotalCost(), 0.01, "Total cost calculation mismatch");
        assertEquals(LedgerEntry.LedgerStatus.PENDING, ledger.getLedgerStatus());

        System.out.println("--- HAPPY PATH TEST PASSED ---");
    }
}