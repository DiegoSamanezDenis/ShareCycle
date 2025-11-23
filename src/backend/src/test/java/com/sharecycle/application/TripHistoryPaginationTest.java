package com.sharecycle.application;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import jakarta.transaction.Transactional;

import com.sharecycle.domain.model.Bike;
import com.sharecycle.domain.model.Bill;
import com.sharecycle.domain.model.LedgerEntry;
import com.sharecycle.domain.model.Operator;
import com.sharecycle.domain.model.Rider;
import com.sharecycle.domain.model.Station;
import com.sharecycle.domain.model.Trip;
import com.sharecycle.domain.repository.JpaLedgerEntryRepository;
import com.sharecycle.infrastructure.persistence.JpaBikeRepository;
import com.sharecycle.infrastructure.persistence.JpaTripRepository;
import com.sharecycle.infrastructure.persistence.JpaUserRepository;

@SpringBootTest
@ActiveProfiles("test")
class TripHistoryPaginationTest {

    @Autowired
    private ListTripsUseCase listTripsUseCase;
    @Autowired
    private JpaTripRepository tripRepository;
    @Autowired
    private JpaLedgerEntryRepository ledgerEntryRepository;
    @Autowired
    private JpaBikeRepository bikeRepository;
    @Autowired
    private JpaUserRepository userRepository;

    private Rider riderOne;
    private Rider riderTwo;
    private Operator operatorUser;
    private final List<UUID> riderOneTripOrder = new ArrayList<>();
    private final List<UUID> riderTwoTripOrder = new ArrayList<>();
    private UUID maintenanceTripId;
    private UUID activeTripId;

    @BeforeEach
    void setUp() {
        riderOneTripOrder.clear();
        riderTwoTripOrder.clear();
        riderOne = createRider("History Rider", "history@example.com", "history");
        riderTwo = createRider("Secondary Rider", "second@example.com", "second");
        operatorUser = createOperator();
        seedTrips();
    }

    @Test
    @Transactional
    void riderHistorySortedAndPagedLatestFirst() {
        ListTripsUseCase.TripHistoryPage pageOne = listTripsUseCase.execute(
                riderOne,
                null,
                null,
                null,
                0,
                8,
                null,
                "RIDER"
        );

        assertThat(pageOne.page()).isEqualTo(0);
        assertThat(pageOne.pageSize()).isEqualTo(8);
        assertThat(pageOne.entries()).hasSize(8);
        assertThat(pageOne.totalItems()).isEqualTo(riderOneTripOrder.size());
        assertThat(pageOne.totalPages()).isEqualTo(2);
        assertThat(pageOne.hasNext()).isTrue();
        assertThat(pageOne.hasPrevious()).isFalse();
        assertDescendingByEndTime(pageOne.entries());
        assertExclusions(pageOne.entries());

        ListTripsUseCase.TripHistoryPage pageTwo = listTripsUseCase.execute(
                riderOne,
                null,
                null,
                null,
                1,
                8,
                null,
                "RIDER"
        );

        assertThat(pageTwo.page()).isEqualTo(1);
        assertThat(pageTwo.entries()).hasSize(riderOneTripOrder.size() - 8);
        assertThat(pageTwo.hasNext()).isFalse();
        assertThat(pageTwo.hasPrevious()).isTrue();
        assertDescendingByEndTime(pageTwo.entries());
        assertExclusions(pageTwo.entries());

        List<UUID> returnedIds = new ArrayList<>(
                pageOne.entries().stream()
                        .map(ListTripsUseCase.TripHistoryEntry::tripId)
                        .toList()
        );
        returnedIds.addAll(
                pageTwo.entries().stream()
                        .map(ListTripsUseCase.TripHistoryEntry::tripId)
                        .toList()
        );
        List<UUID> expectedOrder = new ArrayList<>(riderOneTripOrder);
        Collections.reverse(expectedOrder);
        assertThat(returnedIds).containsExactlyElementsOf(expectedOrder);
    }

    @Test
    @Transactional
    void operatorHistoryShowsAllVisibleTrips() {
        ListTripsUseCase.TripHistoryPage page = listTripsUseCase.execute(
                operatorUser,
                null,
                null,
                null,
                0,
                8,
                null,
                "OPERATOR"
        );

        long expectedTotal = riderOneTripOrder.size() + riderTwoTripOrder.size();
        assertThat(page.totalItems()).isEqualTo(expectedTotal);
        assertThat(page.entries()).allSatisfy(entry -> assertThat(entry.endTime()).isNotNull());
        Set<UUID> riderIds = page.entries().stream()
                .map(ListTripsUseCase.TripHistoryEntry::riderId)
                .collect(Collectors.toSet());
        assertThat(riderIds).containsExactlyInAnyOrder(riderOne.getUserId(), riderTwo.getUserId());
        assertExclusions(page.entries());
    }

    private void assertExclusions(List<ListTripsUseCase.TripHistoryEntry> entries) {
        assertThat(entries).noneMatch(entry -> entry.tripId().equals(maintenanceTripId));
        assertThat(entries).noneMatch(entry -> entry.tripId().equals(activeTripId));
    }

    private void assertDescendingByEndTime(List<ListTripsUseCase.TripHistoryEntry> entries) {
        for (int i = 1; i < entries.size(); i++) {
            LocalDateTime previous = entries.get(i - 1).endTime();
            LocalDateTime current = entries.get(i).endTime();
            if (previous != null && current != null) {
                assertThat(previous).isAfterOrEqualTo(current);
            }
        }
    }

    private void seedTrips() {
        LocalDateTime base = LocalDateTime.now().minusDays(2);
        for (int i = 0; i < 10; i++) {
            LocalDateTime start = base.plusHours(i);
            LocalDateTime end = start.plusMinutes(20);
            Trip trip = persistTrip(riderOne, start, end, Bike.BikeStatus.AVAILABLE, true);
            riderOneTripOrder.add(trip.getTripID());
        }

        for (int i = 0; i < 3; i++) {
            LocalDateTime start = base.plusHours(i).plusMinutes(5);
            LocalDateTime end = start.plusMinutes(15);
            Trip trip = persistTrip(riderTwo, start, end, Bike.BikeStatus.AVAILABLE, true);
            riderTwoTripOrder.add(trip.getTripID());
        }

        maintenanceTripId = persistTrip(
                riderOne,
                base.minusHours(1),
                base.minusHours(1).plusMinutes(10),
                Bike.BikeStatus.MAINTENANCE,
                true
        ).getTripID();

        activeTripId = persistTrip(
                riderOne,
                base.plusHours(12),
                null,
                Bike.BikeStatus.AVAILABLE,
                false
        ).getTripID();
    }

    private Trip persistTrip(Rider rider,
                             LocalDateTime start,
                             LocalDateTime end,
                             Bike.BikeStatus status,
                             boolean createLedger) {
        Bike bike = new Bike();
        bike.setId(UUID.randomUUID());
        bike.setStatus(status);
        bike.setType(Bike.BikeType.STANDARD);
        bikeRepository.save(bike);

        Station startStation = new Station();
        startStation.setName("Start-" + UUID.randomUUID());
        Station endStation = new Station();
        endStation.setName("End-" + UUID.randomUUID());

        Trip trip = new Trip(UUID.randomUUID(), start, end, rider, bike, startStation, endStation);
        tripRepository.save(trip);

        if (createLedger) {
            Bill bill = new Bill(2.00, 1.00, 0.0);
            LedgerEntry ledgerEntry = new LedgerEntry(rider, trip, bill, "PAYG");
            ledgerEntryRepository.save(ledgerEntry);
        }
        return trip;
    }

    private Rider createRider(String name, String email, String username) {
        Rider rider = new Rider();
        rider.setUserId(UUID.randomUUID());
        rider.setFullName(name);
        rider.setEmail(email);
        rider.setUsername(username);
        rider.setStreetAddress("123 Street");
        rider.setRole("RIDER");
        rider.setPasswordHash("hash");
        rider.touchOnCreate();
        userRepository.save(rider);
        return rider;
    }

    private Operator createOperator() {
        Operator operator = new Operator();
        operator.setUserId(UUID.randomUUID());
        operator.setFullName("Ops");
        operator.setEmail("ops@example.com");
        operator.setUsername("ops");
        operator.setStreetAddress("Ops Street");
        operator.setPasswordHash("hash");
        operator.touchOnCreate();
        userRepository.save(operator);
        return operator;
    }
}
