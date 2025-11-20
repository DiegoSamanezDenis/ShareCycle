package com.sharecycle.domain.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class Trip {
    private final UUID tripID;
    private final LocalDateTime startTime;
    private LocalDateTime endTime;
    private int durationMinutes;
    private final Rider rider;
    private final Bike bike;
    private final Station startStation;
    private Station endStation;
    private double appliedDiscountRate = 0.0;

    public Trip(UUID tripID,
                LocalDateTime startTime,
                LocalDateTime endTime,
                Rider rider,
                Bike bike,
                Station startStation,
                Station endStation) {
        this.tripID = Objects.requireNonNullElseGet(tripID, UUID::randomUUID);
        this.startTime = Objects.requireNonNull(startTime, "startTime must not be null");
        this.endTime = endTime;
        this.rider = Objects.requireNonNull(rider, "rider must not be null");
        this.bike = Objects.requireNonNull(bike, "bike must not be null");
        this.startStation = Objects.requireNonNull(startStation, "startStation must not be null");
        this.endStation = endStation;
        recalculateDuration();
    }

    public UUID getTripID() {
        return tripID;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public Rider getRider() {
        return rider;
    }

    public Bike getBike() {
        return bike;
    }

    public Station getStartStation() {
        return startStation;
    }

    public Station getEndStation() {
        return endStation;
    }

    public double getAppliedDiscountRate() {
        return appliedDiscountRate;
    }

    public void setAppliedDiscountRate(double appliedDiscountRate) {
        this.appliedDiscountRate = appliedDiscountRate;
    }

    public void endTrip(Station destination, LocalDateTime endedAt) {
        this.endStation = destination;
        this.endTime = endedAt;
        recalculateDuration();
    }

    private void recalculateDuration() {
        if (startTime != null && endTime != null) {
            long minutes = Math.max(0, Duration.between(startTime, endTime).toMinutes());
            this.durationMinutes = (int) minutes;
        } else {
            this.durationMinutes = 0;
        }
    }
}
