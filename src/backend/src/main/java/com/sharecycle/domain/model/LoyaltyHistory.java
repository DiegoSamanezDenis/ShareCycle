package com.sharecycle.domain.model;

import java.util.UUID;
import java.time.LocalDateTime;
public class LoyaltyHistory {
    private UUID id;
    private UUID riderId;
    private LoyaltyTier tier;
    private LocalDateTime reachedAt;
    private String reason;

    public LoyaltyHistory(UUID id, UUID riderId, LoyaltyTier tier, LocalDateTime reachedAt, String reason) {
        this.id = id == null ? UUID.randomUUID() : id;
        this.riderId = riderId;
        this.tier = tier;
        this.reachedAt = reachedAt == null ? LocalDateTime.now() : reachedAt;
        this.reason = reason;
    }

    public UUID getId() {
        return id;
    }

    public UUID getRiderId() {
        return riderId;
    }

    public LoyaltyTier getTier() {
        return tier;
    }

    public LocalDateTime getReachedAt() {
        return reachedAt;
    }

    public String getReason() {
        return reason;
    }
}
