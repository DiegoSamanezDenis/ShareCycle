package com.sharecycle.infrastructure.persistence.jpa;

import com.sharecycle.domain.model.LoyaltyHistory;
import com.sharecycle.domain.model.LoyaltyTier;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "loyalty_history")
public class JpaLoyaltyHistoryEntity {

    @Id
    @Column(name = "history_id", columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "rider_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID riderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier", nullable = false)
    private LoyaltyTier tier;

    @Column(name = "reached_at", nullable = false)
    private LocalDateTime reachedAt;

    @Column(name = "reason", nullable = false)
    private String reason;

    public JpaLoyaltyHistoryEntity() {
    }

    public JpaLoyaltyHistoryEntity(LoyaltyHistory loyaltyHistory) {
        this.id = loyaltyHistory.getId();
        this.riderId = loyaltyHistory.getRiderId();
        this.tier = loyaltyHistory.getTier();
        this.reachedAt = loyaltyHistory.getReachedAt();
        this.reason = loyaltyHistory.getReason();
    }

    public LoyaltyHistory toDomain() {
        return new LoyaltyHistory(
                this.id,
                this.riderId,
                this.tier,
                this.reachedAt,
                this.reason
        );
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

    public void setId(UUID id) {
        this.id = id;
    }

    public void setRiderId(UUID riderId) {
        this.riderId = riderId;
    }

    public void setTier(LoyaltyTier tier) {
        this.tier = tier;
    }

    public void setReachedAt(LocalDateTime reachedAt) {
        this.reachedAt = reachedAt;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
