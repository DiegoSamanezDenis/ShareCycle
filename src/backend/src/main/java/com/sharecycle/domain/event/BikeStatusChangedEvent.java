package com.sharecycle.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record BikeStatusChangedEvent(
        UUID bikeId,
        String previousStatus,
        String newStatus,
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
