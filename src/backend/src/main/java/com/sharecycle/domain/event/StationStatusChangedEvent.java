package com.sharecycle.domain.event;

import com.sharecycle.domain.model.Station;

import java.time.LocalDateTime;
import java.util.UUID;

public record StationStatusChangedEvent(UUID stationId, Station.StationStatus status, int capacity, int bikesDocked) implements DomainEvent {
    @Override
    public LocalDateTime occurredAt() {
        return LocalDateTime.now();
    }
}
