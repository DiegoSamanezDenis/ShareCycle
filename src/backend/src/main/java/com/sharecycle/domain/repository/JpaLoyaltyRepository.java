package com.sharecycle.domain.repository;

import com.sharecycle.domain.model.LoyaltyHistory;
import com.sharecycle.domain.model.LoyaltyTier;
import java.util.UUID;
import java.util.List;

public interface JpaLoyaltyRepository {
    void save(LoyaltyHistory loyaltyHistory);

    List<LoyaltyHistory> findHistoryByRiderId(UUID riderId);

    LoyaltyTier findCurrentTier(UUID riderId);
}
