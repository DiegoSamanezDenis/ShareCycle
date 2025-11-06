package com.sharecycle.domain.event;

import java.util.UUID;
import java.time.LocalDateTime;

public record TripBilledEvent (
        UUID tripId,
        UUID ledgerId
) implements DomainEvent {
    @Override
    public LocalDateTime occurredAt() {
        return LocalDateTime.now();
    }
}
