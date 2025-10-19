package com.sharecycle.infrastructure.persistence.jpa;

import com.sharecycle.domain.model.Rider;
import com.sharecycle.domain.model.Trip;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "trips")
public class JpaTripEntity {

    @Id
    @Column(name = "trip_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID tripId;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "duration_minutes")
    private int durationMinutes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private JpaUserEntity.JpaRiderEntity rider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bike_id", nullable = false)
    private JpaBikeEntity bike;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "start_station_id", nullable = false)
    private JpaStationEntity startStation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "end_station_id")
    private JpaStationEntity endStation;

    public JpaTripEntity() {
    }

    private JpaTripEntity(Trip trip, MapperContext context) {
        this.tripId = trip.getTripID();
        this.startTime = trip.getStartTime();
        this.endTime = trip.getEndTime();
        this.durationMinutes = trip.getDurationMinutes();
        this.rider = (JpaUserEntity.JpaRiderEntity) JpaUserEntity.fromDomain(trip.getRider());
        this.bike = JpaBikeEntity.fromDomain(trip.getBike(), context);
        this.startStation = JpaStationEntity.fromDomain(trip.getStartStation(), context);
        if (trip.getEndStation() != null) {
            this.endStation = JpaStationEntity.fromDomain(trip.getEndStation(), context);
        }
    }

    public static JpaTripEntity fromDomain(Trip trip, MapperContext context) {
        JpaTripEntity existing = context.tripEntities.get(trip.getTripID());
        if (existing != null) {
            return existing;
        }
        context.trips.put(trip.getTripID(), trip);
        JpaTripEntity entity = new JpaTripEntity(trip, context);
        context.tripEntities.put(trip.getTripID(), entity);
        return entity;
    }

    public Trip toDomain(MapperContext context) {
        Trip existing = context.trips.get(tripId);
        if (existing != null) {
            return existing;
        }
        Rider riderDomain = rider.toDomain();
        Trip trip = new Trip(
                tripId,
                startTime,
                endTime,
                riderDomain,
                bike.toDomain(context),
                startStation.toDomain(context),
                endStation != null ? endStation.toDomain(context) : null
        );
        context.trips.put(tripId, trip);
        return trip;
    }

    public UUID getTripId() {
        return tripId;
    }

    public void setTripId(UUID tripId) {
        this.tripId = tripId;
    }
}
