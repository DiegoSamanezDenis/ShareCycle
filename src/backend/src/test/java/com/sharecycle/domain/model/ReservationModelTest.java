package com.sharecycle.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class ReservationModelTest {

    @Test
    void isExpired_returnsTrueWhenExpiresAtInPast() {
        Instant now = Instant.now();
        Instant expiredAt = now.minus(5, ChronoUnit.MINUTES);

        Reservation r = new Reservation(UUID.randomUUID(),
                new Rider(), // assuming default constructor exists
                new Station(),
                new Bike(Bike.BikeType.STANDARD),
                now.minus(10, ChronoUnit.MINUTES),
                expiredAt,
                5,
                true);

        assertThat(r.isExpired()).isTrue();
        assertThat(r.isActive()).isFalse(); // active should be false when expired
    }

    @Test
    void isActive_returnsTrueWhenNotExpiredAndMarkedActive() {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(10, ChronoUnit.MINUTES);

        Reservation r = new Reservation(UUID.randomUUID(),
                new Rider(),
                new Station(),
                new Bike(Bike.BikeType.STANDARD),
                now,
                expiresAt,
                10,
                true);

        assertThat(r.isExpired()).isFalse();
        assertThat(r.isActive()).isTrue();
    }

    @Test
    void expire_setsActiveToFalse() {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(5, ChronoUnit.MINUTES);

        Reservation r = new Reservation(UUID.randomUUID(),
                new Rider(),
                new Station(),
                new Bike(Bike.BikeType.STANDARD),
                now,
                expiresAt,
                5,
                true);

        assertThat(r.isMarkedActive()).isTrue();
        r.expire();
        assertThat(r.isMarkedActive()).isFalse();
        // after expire, isActive should be false regardless of expiresAt (even if not passed)
        assertThat(r.isActive()).isFalse();
    }
}
