package com.sharecycle.domain.event;

import java.util.UUID;

public record BikeMovedEvent(UUID bikeId, UUID sourceStationId, UUID destinationStationId) implements DomainEvent {
    @Override
    public java.time.LocalDateTime occurredAt() {
        return java.time.LocalDateTime.now();
    }
}
