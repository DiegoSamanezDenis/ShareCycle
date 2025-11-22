package com.sharecycle.application;

import com.sharecycle.domain.model.*;
import com.sharecycle.domain.TripBuilder;
import com.sharecycle.domain.repository.JpaLedgerEntryRepository;
import com.sharecycle.domain.repository.JpaBikeRepository;
import com.sharecycle.domain.repository.JpaStationRepository;
import com.sharecycle.infrastructure.persistence.JpaTripRepository;
import com.sharecycle.infrastructure.persistence.JpaUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional

public class StationFullTest {

    @Autowired
    private BmsFacade bmsFacade;

    @Autowired
    private JpaUserRepository userRepository;

    @Autowired
    private JpaStationRepository stationRepository;

    @Autowired
    private JpaBikeRepository bikeRepository;

    @Autowired
    private JpaTripRepository tripRepository;

    @Autowired
    private JpaLedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private PaymentUseCase paymentUseCase;

    @Test
    public void overflowCreditForFullStationReturn() {
        Rider rider = createRider("Test Rider", "testrider@example.com");
        double initialFlexCredit = rider.getFlexCredit();
        Bike tripBike = createBike();
        Station fullStation = createFullStation("Full Destination Station", 3);
        Trip activeTrip = createActiveTrip(rider, tripBike, fullStation);

        assertFalse(fullStation.hasFreeDock(), "Destination station has no free docks");
        assertEquals(0, fullStation.getFreeDockCount(), "Free dock count should be zero");

        BmsFacade.TripCompletionResult result = bmsFacade.endTrip(activeTrip.getTripID(), fullStation.getId());

        assertNotNull(result, "Result should not be null");
        assertTrue(result.isBlocked(), "Expected a blocked trip completion result");
        assertFalse(result.isCompleted(), "Trip should not be marked as completed");

        BmsFacade.ReturnBlockInfo blockInfo = result.blockInfo();
        assertNotNull(blockInfo, "ReturnBlockInfo should be present");
        assertTrue(blockInfo.hasCredit(), "Expected a courtesy credit to be present in block info");

        LedgerEntry creditEntry = blockInfo.creditLedgerEntry();
        assertNotNull(creditEntry, "Credit LedgerEntry should be returned in the block info");
        assertEquals("RETURN_BLOCK_CREDIT", creditEntry.getPricingPlan(), "Ledger entry should have RETURN_BLOCK_CREDIT pricing plan");
        assertEquals(rider.getUserId(), creditEntry.getUser().getUserId(), "Credit should be associated with the rider");
        assertNull(creditEntry.getTrip(), "Credit ledger entry should not be tied to a specific trip");

        Bill creditBill = creditEntry.getBill();
        assertNotNull(creditBill, "Credit entry should have a Bill");
        assertTrue(creditBill.getTotalCost() < 0, "Bill total should be negative to represent a credit");
        assertEquals(-1.00, creditBill.getTotalCost(), 0.001, "Credit amount should be -$1.00");

        String description = creditEntry.getDescription();
        assertNotNull(description, "Credit entry should have a description");
        assertTrue(description.contains(activeTrip.getTripID().toString()), "Description should contain the trip ID");
        assertTrue(description.toLowerCase().contains("credit"), "Description should mention 'credit");

        List<LedgerEntry> userEntries = ledgerEntryRepository.findAllByUser(rider);
        assertNotNull(userEntries, "Ledger entries result should not be null");
        
        boolean foundCredit = userEntries.stream().anyMatch(entry -> "RETURN_BLOCK_CREDIT".equals(entry.getPricingPlan())
            && entry.getBill() != null && entry.getBill().getTotalCost() < 0.0);

        assertTrue(foundCredit, "Expected to find a persisted RETURN_BLOCK_CREDIT ledger entry with a neg bill total");

        long creditCount = userEntries.stream().filter(e -> "RETURN_BLOCK_CREDIT".equals(e.getPricingPlan())).count();
        assertEquals(1, creditCount, "Should have exactly one RETURN_BLOCK_CREDIT entry");

        Rider updatedRider = (Rider) userRepository.findById(rider.getUserId());
        assertNotNull(updatedRider, "Updated rider should be found in database");

        LedgerEntry persistedCredit = userEntries.stream().filter(e -> "RETURN_BLOCK_CREDIT".equals(e.getPricingPlan())).findFirst().orElse(null);
        assertNotNull(persistedCredit, "Persisted credit entry should exist");
        assertEquals(-1.00, persistedCredit.getBill().getTotalCost(), 0.001, "Persisted credit should have -$1.00 value");

        paymentUseCase.execute(persistedCredit);

        Rider finalRider = (Rider) userRepository.findById(rider.getUserId());
        double expectedBalance = initialFlexCredit + 1.00;
        assertEquals(expectedBalance, finalRider.getFlexCredit(), 0.001, "Rider's flex credit balance should be $1.00");

        LedgerEntry processedCredit = ledgerEntryRepository.findById(creditEntry.getLedgerId());
        assertEquals(LedgerEntry.LedgerStatus.PAID, processedCredit.getLedgerStatus(), "Credit ledger should be marked as PAID after processing");

    }

    @Test
    public void overflowCreditAppliedOnlyOnce() {
        Rider rider = createRider("Duplicate test rider", "duplicate@email.com");
        Bike tripBike = createBike();
        Station fullStation = createFullStation("Full Station 2", 2);
        Trip activeTrip = createActiveTrip(rider, tripBike, fullStation);

        BmsFacade.TripCompletionResult first = bmsFacade.endTrip(activeTrip.getTripID(), fullStation.getId());
        assertTrue(first.isBlocked(), "First attempt should be blocked");
        assertTrue(first.blockInfo().hasCredit(), "First attempt should produce a credit");

        List<LedgerEntry> entriesAfterFirst = ledgerEntryRepository.findAllByUser(rider);
        long creditCountAfterFirst = entriesAfterFirst.stream().filter(e -> "RETURN_BLOCK_CREDIT".equals(e.getPricingPlan())).count();
        assertEquals(1, creditCountAfterFirst, "Should have exactly one credit after first attempt");

        UUID firstCreditId = first.blockInfo().creditLedgerEntry().getLedgerId();

        BmsFacade.TripCompletionResult second = bmsFacade.endTrip(activeTrip.getTripID(), fullStation.getId());
        assertTrue(second.isBlocked(), "Second attempt should still be blocked");
        assertTrue(second.blockInfo().hasCredit(), "Second attempt should return the existing credit");

        List<LedgerEntry> entriesAfterSecond = ledgerEntryRepository.findAllByUser(rider);
        long creditCountAfterSecond = entriesAfterSecond.stream().filter(e -> "RETURN_BLOCK_CREDIT".equals(e.getPricingPlan())).count();

        assertEquals(creditCountAfterFirst, creditCountAfterSecond, "Duplicated RETURN_BLOCK_CREDIT entries shouldn't be created");
        assertEquals(1, creditCountAfterSecond, "Should still have exactly one credit entry after second attempt");

        UUID secondCreditId = second.blockInfo().creditLedgerEntry().getLedgerId();
        assertEquals(firstCreditId, secondCreditId, "Both attempts should return the same credit ledger entry");
        
    }

    @Test
    public void flexCreditEventEmittedDuringPayment() {
        Rider rider = createRider("Payment rider", "payment@example.com");
        double initialFlexCredit = rider.getFlexCredit();
        Bike tripBike = createBike();
        Station fullStation = createFullStation("Full Station 3", 3);
        Trip activeTrip = createActiveTrip(rider, tripBike, fullStation);

        BmsFacade.TripCompletionResult blockedResult = bmsFacade.endTrip(activeTrip.getTripID(), fullStation.getId()); 
        assertTrue(blockedResult.isBlocked(), "Trip return should be blocked");
        assertTrue(blockedResult.blockInfo().hasCredit(), "Credit should be created");

        LedgerEntry creditEntry = blockedResult.blockInfo().creditLedgerEntry();
        assertNotNull(creditEntry, "Credit ledger entry should exist");
        assertEquals(-1.00, creditEntry.getBill().getTotalCost(), 0.001, "Credit should be $1.00");

        List<LedgerEntry> userEntries = ledgerEntryRepository.findAllByUser(rider);
        boolean hasPendingCredit = userEntries.stream().anyMatch(e -> "RETURN_BLOCK_CREDIT".equals(e.getPricingPlan())
            && e.getBill().getTotalCost() < 0);
        assertTrue(hasPendingCredit, "Rider should have a pending credit ledger entry to be applied during payment");

       }

    private Rider createRider(String fullName, String email) {
        Rider r = new Rider();
        r.setUserId(UUID.randomUUID());
        r.setFullName(fullName);
        r.setEmail(email);
        r.setRole("RIDER");
        r.setUsername(email);
        r.setPasswordHash("password");
        r.setStreetAddress("123 Test Street");
        r.setFlexCredit(0.0);
        r.touchOnCreate();
        userRepository.save(r);
        return r;
    }

    private Bike createBike() {
        Bike b = new Bike();
        b.setId(UUID.randomUUID());
        b.setStatus(Bike.BikeStatus.AVAILABLE);
        b.setType(Bike.BikeType.STANDARD);
        bikeRepository.save(b);
        return b;
    }

    private Station createEmptyStation(String name, int capacity) {
        Station s = new Station();
        s.setId(UUID.randomUUID());
        s.setName(name);
        s.setLatitude(45.5 + Math.random() * 0.1);
        s.setLongitude(-73.6 + Math.random() * 0.1);
        s.markActive();
        List<Dock> docks = new ArrayList<>();
        for (int i = 0; i < capacity; i++) {
            Dock d = new Dock();
            d.setId(UUID.randomUUID());
            d.setStation(s);
            d.setStatus(Dock.DockStatus.EMPTY);
            docks.add(d);
        }
        s.setDocks(docks);
        stationRepository.save(s);
        return s;
    }

    private Station createFullStation(String name, int capacity) {
        Station s = new Station();
        s.setId(UUID.randomUUID());
        s.setName(name);
        s.setLatitude(45.5 + Math.random() * 0.1);
        s.setLongitude(-73.6 + Math.random() * 0.1);
        s.markActive();
        List<Dock> docks = new ArrayList<>();
        for (int i = 0; i < capacity; i++) {
            Bike dockBike = new Bike();
            dockBike.setId(UUID.randomUUID());
            dockBike.setStatus(Bike.BikeStatus.AVAILABLE);
            dockBike.setType(Bike.BikeType.STANDARD);
            bikeRepository.save(dockBike);

            Dock dock = new Dock();
            dock.setId(UUID.randomUUID());
            dock.setStation(s);
            dock.setStatus(Dock.DockStatus.OCCUPIED);
            dock.setOccupiedBike(dockBike);
            docks.add(dock);
        }
        
        s.setDocks(docks);
        s.updateBikesDocked();
        stationRepository.save(s);
        return s;
    }

    private Trip createActiveTrip(Rider rider, Bike bike, Station startStation) {
        TripBuilder builder = new TripBuilder();
        builder.setTripId(UUID.randomUUID());
        builder.setRider(rider);
        builder.setBike(bike);
        builder.setStartStation(startStation);
        builder.setStartTime(LocalDateTime.now().minusMinutes(15));
        bike.setStatus(Bike.BikeStatus.ON_TRIP);
        bikeRepository.save(bike);
        Trip t = builder.build();
        tripRepository.save(t);
        return t;
    }

}