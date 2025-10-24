package com.sharecycle.infrastructure.persistence.jpa;

import com.sharecycle.domain.event.BikeMovedEvent;
import com.sharecycle.domain.event.ReservationCreatedEvent;
import com.sharecycle.domain.event.StationCapacityChangedEvent;
import com.sharecycle.domain.event.StationStatusChangedEvent;
import com.sharecycle.domain.event.TripBilledEvent;
import com.sharecycle.domain.event.TripEndedEvent;
import com.sharecycle.domain.event.TripStartedEvent;
import com.sharecycle.domain.model.Bike;
import com.sharecycle.domain.model.Rider;
import com.sharecycle.domain.model.Station;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "domain_event")
public class JpaDomainEventEntity {

    @Id
    @Column(name = "event_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "message", length = 500)
    private String message;

    @Column(name = "payload")
    private String payload;

    @Column(name = "principal_user_id", columnDefinition = "BINARY(16)")
    private UUID principalUserId;

    @Column(name = "station_id", columnDefinition = "BINARY(16)")
    private UUID stationId;

    @Column(name = "bike_id", columnDefinition = "BINARY(16)")
    private UUID bikeId;

    @Column(name = "trip_id", columnDefinition = "BINARY(16)")
    private UUID tripId;

    @Transient
    private static final int MESSAGE_LIMIT = 500;

    public JpaDomainEventEntity() {
    }

    private JpaDomainEventEntity(UUID eventId,
                                 String eventType,
                                 Instant occurredAt,
                                 String message,
                                 String payload,
                                 UUID principalUserId,
                                 UUID stationId,
                                 UUID bikeId,
                                 UUID tripId) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.occurredAt = occurredAt;
        this.message = message;
        this.payload = payload;
        this.principalUserId = principalUserId;
        this.stationId = stationId;
        this.bikeId = bikeId;
        this.tripId = tripId;
    }

    public static JpaDomainEventEntity fromEvent(Object event) {
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }
        UUID eventId = UUID.randomUUID();
        String eventType = event.getClass().getSimpleName();
        Instant occurredAt = Instant.now();
        String message = truncate(event.toString(), MESSAGE_LIMIT);

        UUID principalUserId = null;
        UUID stationId = null;
        UUID bikeId = null;
        UUID tripId = null;

        if (event instanceof TripStartedEvent started) {
            tripId = started.tripID();
            Rider rider = started.rider();
            if (rider != null) {
                principalUserId = rider.getUserId();
            }
            Bike bike = started.bike();
            if (bike != null) {
                bikeId = bike.getId();
            }
            Station station = started.startStation();
            if (station != null) {
                stationId = station.getId();
            }
        } else if (event instanceof TripEndedEvent ended) {
            tripId = ended.tripId();
        } else if (event instanceof TripBilledEvent billed) {
            tripId = billed.tripId();
        } else if (event instanceof ReservationCreatedEvent created) {
            principalUserId = created.getRiderId();
        } else if (event instanceof StationStatusChangedEvent statusChanged) {
            stationId = statusChanged.stationId();
        } else if (event instanceof StationCapacityChangedEvent capacityChanged) {
            stationId = capacityChanged.stationId();
        } else if (event instanceof BikeMovedEvent bikeMoved) {
            bikeId = bikeMoved.bikeId();
            stationId = bikeMoved.destinationStationId();
        }

        return new JpaDomainEventEntity(
                eventId,
                eventType,
                occurredAt,
                message,
                null,
                principalUserId,
                stationId,
                bikeId,
                tripId
        );
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getMessage() {
        return message;
    }

    public String getPayload() {
        return payload;
    }

    public UUID getPrincipalUserId() {
        return principalUserId;
    }

    public UUID getStationId() {
        return stationId;
    }

    public UUID getBikeId() {
        return bikeId;
    }

    public UUID getTripId() {
        return tripId;
    }
}
