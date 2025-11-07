package com.sharecycle.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentStartedEvent(
        UUID riderId,
        UUID tripId,
        String msg,
        LocalDateTime occurredAt
) implements DomainEvent {
    public PaymentStartedEvent(UUID riderId, UUID tripId, String msg) {
        this(riderId, tripId, msg, LocalDateTime.now());
    }
}
