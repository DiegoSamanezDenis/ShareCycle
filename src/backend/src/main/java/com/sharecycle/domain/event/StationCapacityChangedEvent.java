package com.sharecycle.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record StationCapacityChangedEvent(
        UUID stationId,
        int capacity,
        int bikesDocked,
        int freeDocks
) implements DomainEvent {
    @Override
    public LocalDateTime occurredAt() {
        return LocalDateTime.now();
    }
}
