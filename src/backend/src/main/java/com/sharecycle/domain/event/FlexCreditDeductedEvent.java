package com.sharecycle.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record FlexCreditDeductedEvent(
        UUID userId,
        double amount,
        LocalDateTime occuredAt
) implements DomainEvent {
    public FlexCreditDeductedEvent(UUID userId, double amount) {
        this(userId, amount, LocalDateTime.now());
    }

    @Override
    public LocalDateTime occurredAt() {
        return occuredAt;
    }
}
