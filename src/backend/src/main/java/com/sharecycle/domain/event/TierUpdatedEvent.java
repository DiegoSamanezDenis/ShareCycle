package com.sharecycle.domain.event;

import com.sharecycle.domain.model.LoyaltyTier;
import java.time.LocalDateTime;
import java.util.UUID;

public record TierUpdatedEvent(
    UUID riderId,
    LoyaltyTier oldTier,
    LoyaltyTier newTier,
    String reason,
    LocalDateTime occurredAt
) implements DomainEvent {
    public TierUpdatedEvent(UUID riderId, LoyaltyTier oldTier, LoyaltyTier newTier, String reason) {
        this(riderId, oldTier, newTier, reason, LocalDateTime.now());
    }
}
