package com.sharecycle.domain.model;

import java.time.Instant;
import java.util.UUID;

public class Reservation {

    private final UUID reservationId;
    private final Rider rider;
    private final Station station;
    private final Bike bike;
    private final Instant reservedAt;
    private final Instant expiresAt;
    private final int expiresAfterMinutes;
    private boolean active;

    public Reservation(UUID reservationId,
                       Rider rider,
                       Station station,
                       Bike bike,
                       Instant reservedAt,
                       Instant expiresAt,
                       int expiresAfterMinutes,
                       boolean active) {
        this.reservationId = reservationId == null ? UUID.randomUUID() : reservationId;
        this.rider = rider;
        this.station = station;
        this.bike = bike;
        this.reservedAt = reservedAt;
        this.expiresAt = expiresAt;
        this.expiresAfterMinutes = expiresAfterMinutes;
        this.active = active;
    }

    public UUID getReservationId() {
        return reservationId;
    }

    public Rider getRider() {
        return rider;
    }

    public Station getStation() {
        return station;
    }

    public Bike getBike() {
        return bike;
    }

    public Instant getReservedAt() {
        return reservedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public int getExpiresAfterMinutes() {
        return expiresAfterMinutes;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isActive() {
        return active && !isExpired();
    }

    public boolean isMarkedActive() {
        return active;
    }

    public void expire() {
        this.active = false;
    }
}
