package com.sharecycle.model.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@DiscriminatorValue("Reservation")
public class Reservation {

    @Id
    @GeneratedValue
    private UUID reservationId;

    @ManyToOne
    @JoinColumn(name = "rider_id")
    private Rider rider;

    // @ManyToOne
    // @JoinColumn(name = "station_id")
    // private Station station;

    // @ManyToOne
    // @JoinColumn(name = "bike_id")
    // private Bike bike;

    private Instant reservedAt;
    private Instant expiresAt;
    private int expiresAfterMinutes;
    private boolean active;

    // Required by JPA
    public Reservation() {}

    public Reservation(UUID reservationId, Rider rider, Instant reservedAt, Instant expiresAt, int expiresAfterMinutes, boolean active) {
        this.reservationId = reservationId;
        this.rider = rider;
        this.reservedAt = reservedAt;
        this.expiresAt = expiresAt;
        this.expiresAfterMinutes = expiresAfterMinutes;
        this.active = active;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isActive() {

        return active && !isExpired();
    }

    public void expire() {

        this.active = false;
    }

    public UUID getReservationId() {

        return reservationId;
    }

    public Rider getRider() {

        return rider;
    }

    public UUID getRiderId() {

        return rider.getUserId();
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

    public boolean getActive() {

        return active;
    }

    // Getters for station and bike can be added later
}
