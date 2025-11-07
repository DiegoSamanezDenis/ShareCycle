package com.sharecycle.domain.event;

import com.sharecycle.domain.model.Bike;

import java.time.LocalDateTime;
import java.util.UUID;

public record BikeStatusChangedEvent(
        UUID bikeId,
        Bike.BikeStatus status,
        UUID stationId,
        LocalDateTime occurredAt
) implements DomainEvent {
    public BikeStatusChangedEvent {
        if (occurredAt == null) {
            occurredAt = LocalDateTime.now();
        }
    }

    @Override
    public LocalDateTime occurredAt() {
        return occurredAt;
    }
}
