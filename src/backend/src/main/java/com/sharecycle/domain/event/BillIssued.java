package com.sharecycle.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record BillIssued(
        UUID billId,
        UUID tripId,
        UUID ledgerId,
        long amountCents,
        String currency,
        String description,
        LocalDateTime occurredAt
) implements DomainEvent {
    public BillIssued {
        if (occurredAt == null) {
            occurredAt = LocalDateTime.now();
        }
        if (currency == null) {
            currency = "USD";
        }
        if (description == null) {
            description = "";
        }
    }

    @Override
    public LocalDateTime occurredAt() {
        return occurredAt;
    }
}