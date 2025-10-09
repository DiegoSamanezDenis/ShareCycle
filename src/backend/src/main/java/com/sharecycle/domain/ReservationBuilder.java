package com.sharecycle.domain;

import java.time.Instant;
import java.util.UUID;

import com.sharecycle.model.entity.Bike;
import com.sharecycle.model.entity.Rider;
import com.sharecycle.model.entity.Reservation;
import com.sharecycle.model.entity.Station;

public class ReservationBuilder {
    private Rider rider;
    private Station station;
    private Bike bike;
    private int expiresAfterMinutes;

    public ReservationBuilder rider(Rider rider) {
        this.rider = rider;
        return this;
    }

    public ReservationBuilder station(Station station) {
        this.station = station;
        return this;
    }

    public ReservationBuilder bike(Bike bike) {
        this.bike = bike;
        return this;
    }

    public ReservationBuilder expiresAfterMinutes(int expiresAfterMinutes) {
        this.expiresAfterMinutes = expiresAfterMinutes;
        return this;
    }

    public Reservation build() {
        if (rider == null) {
            throw new IllegalArgumentException("Rider cannot be null when creating a reservation.");
        }
        if (expiresAfterMinutes <= 0) {
            throw new IllegalArgumentException("Expires-after time must be greater than 0 minutes.");
        }

        Instant reservedAt = Instant.now();
        Instant expiresAt = reservedAt.plusSeconds(expiresAfterMinutes * 60L);

        return new Reservation(UUID.randomUUID(), rider, station, bike, reservedAt, expiresAt, expiresAfterMinutes, true);
    }
}
