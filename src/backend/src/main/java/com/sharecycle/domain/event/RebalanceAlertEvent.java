package com.sharecycle.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when a station becomes empty and requires rebalancing.
 * Operators are notified to redistribute bikes from other stations.
 */
public record RebalanceAlertEvent(
    UUID stationId,
    String stationName,
    String address,
    double latitude,
    double longitude,
    int capacity
) implements DomainEvent {
    @Override
    public LocalDateTime occurredAt() {
        return LocalDateTime.now();
    }
}
