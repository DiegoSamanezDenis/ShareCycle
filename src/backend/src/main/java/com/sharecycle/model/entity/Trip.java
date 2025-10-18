package com.sharecycle.model.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "trips")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)

public class Trip {
    @Id
    @GeneratedValue
    @Column(name = "trip_id", columnDefinition = "BINARY(16)")
    private UUID tripID; // primary key

    @Column(name = "startTime", columnDefinition = "TIMESTAMP")
    private LocalDateTime startTime;

    @Column(name = "endTime", columnDefinition = "TIMESTAMP", nullable = true)
    private LocalDateTime endTime;

    @Column(name = "durationMinutes", nullable = false)
    private int durationMinutes;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "user_id")
    private Rider rider;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "bike_id")
    private Bike bike;

    @ManyToOne
    @JoinColumn(name = "start_station_id", nullable = false)
    private Station startStation;

    @ManyToOne
    @JoinColumn(name = "end_station_id", nullable = true)
    private Station endStation;


    public Trip() {}

    public Trip(UUID tripID, LocalDateTime startTime, LocalDateTime endTime, Rider rider, Bike bike, Station startStation, Station endStation) {
        this.tripID = Objects.requireNonNullElseGet(tripID, UUID::randomUUID);
        this.startTime = startTime;
        this.endTime = endTime;
        if (endTime == null) {
            this.durationMinutes = 0;
        } else {
            this.durationMinutes = endTime.getMinute()-startTime.getMinute();
        }
        this.rider = rider;
        this.bike = bike;
        this.startStation = startStation;
        this.endStation = endStation;
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

    @Override
    public String toString() {
        return "Trip{" +
                "tripID=" + tripID +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", durationMinutes=" + durationMinutes +
                ", rider=" + rider +
                ", bike=" + bike +
                ", startStation=" + startStation +
                ", endStation=" + endStation +
                '}';
    }
}
