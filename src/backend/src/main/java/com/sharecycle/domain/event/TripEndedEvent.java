package com.sharecycle.domain.event;

import com.sharecycle.domain.model.*;

import java.time.LocalDateTime;
import java.util.UUID;

public record TripEndedEvent (
        UUID tripId
) implements DomainEvent {
    @Override
    public LocalDateTime occurredAt() {
        return LocalDateTime.now();
    }
}
