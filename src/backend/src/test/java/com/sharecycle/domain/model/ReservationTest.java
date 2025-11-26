package com.sharecycle.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class ReservationTest {

    @Test
    void constructorAndGetters() {
        UUID id = UUID.randomUUID();
        Rider rider = new Rider();
        Station station = new Station();
        Bike bike = new Bike();
        Instant now = Instant.now();
        Instant later = now.plusSeconds(60);

        Reservation r = new Reservation(id, rider, station, bike, now, later, 60, true);

        assertThat(r.getReservationId()).isEqualTo(id);
        assertThat(r.getRider()).isEqualTo(rider);
        assertThat(r.getStation()).isEqualTo(station);
        assertThat(r.getBike()).isEqualTo(bike);
        assertThat(r.getReservedAt()).isEqualTo(now);
        assertThat(r.getExpiresAt()).isEqualTo(later);
        assertThat(r.getExpiresAfterMinutes()).isEqualTo(60);
        assertThat(r.isMarkedActive()).isTrue();
        assertThat(r.isActive()).isTrue();

        r.expire();
        assertThat(r.isMarkedActive()).isFalse();
        assertThat(r.isActive()).isFalse();
    }

    @Test
    void isExpiredWorks() {
        Reservation r = new Reservation(null, new Rider(), new Station(), new Bike(), Instant.now().minusSeconds(3600), Instant.now().minusSeconds(10), 10, true);
        assertThat(r.isExpired()).isTrue();
        assertThat(r.isActive()).isFalse();
    }
}

class BillTest {

    @Test
    void constructorsAndGetters() {
        Bill b1 = new Bill();
        assertThat(b1.getBillId()).isNotNull();
        assertThat(b1.getComputedAt()).isNotNull();

        Bill b2 = new Bill(10, 20, 5);
        assertThat(b2.getTotalCost()).isEqualTo(35);
        assertThat(b2.getBaseCost()).isEqualTo(10);
        assertThat(b2.getTimeCost()).isEqualTo(20);
        assertThat(b2.getEBikeSurcharge()).isEqualTo(5);

        Bill b3 = new Bill(null, null, 5, 5, 5, 15,0);
        assertThat(b3.getTotalCost()).isEqualTo(15);
        assertThat(b3.getBillId()).isNotNull();
        assertThat(b3.getComputedAt()).isNotNull();
    }
}

class TripTest {

    @Test
    void constructorAndGetters() {
        Rider rider = new Rider();
        Bike bike = new Bike();
        Station start = new Station();
        Station end = new Station();
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusMinutes(30);

        Trip trip = new Trip(null, startTime, endTime, rider, bike, start, end);
        assertThat(trip.getTripID()).isNotNull();
        assertThat(trip.getStartTime()).isEqualTo(startTime);
        assertThat(trip.getEndTime()).isEqualTo(endTime);
        assertThat(trip.getDurationMinutes()).isEqualTo(30);

        Station newEnd = new Station();
        trip.endTrip(newEnd, endTime.plusMinutes(15));
        assertThat(trip.getEndStation()).isEqualTo(newEnd);
        assertThat(trip.getDurationMinutes()).isEqualTo(45);
    }
}

class LedgerEntryTest {

    @Test
    void constructorsAndGettersSetters() {
        User user = new User();
        Trip trip = new Trip(null, LocalDateTime.now(), LocalDateTime.now().plusMinutes(10), new Rider(), new Bike(), new Station(), new Station());
        Bill bill = new Bill(5, 10, 0);

        LedgerEntry le1 = new LedgerEntry(user, trip, bill, "plan1");
        assertThat(le1.getLedgerId()).isNotNull();
        assertThat(le1.getLedgerStatus()).isEqualTo(LedgerEntry.LedgerStatus.PENDING);

        le1.markAsPaid();
        assertThat(le1.getLedgerStatus()).isEqualTo(LedgerEntry.LedgerStatus.PAID);

        le1.setDescription("desc");
        assertThat(le1.getDescription()).isEqualTo("desc");
    }
}
