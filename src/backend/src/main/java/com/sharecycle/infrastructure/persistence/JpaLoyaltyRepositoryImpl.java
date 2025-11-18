package com.sharecycle.infrastructure.persistence;

import com.sharecycle.domain.model.LoyaltyHistory;
import com.sharecycle.domain.model.LoyaltyTier;
import com.sharecycle.domain.repository.JpaLoyaltyRepository;
import com.sharecycle.infrastructure.persistence.jpa.JpaLoyaltyHistoryEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@Transactional
public class JpaLoyaltyRepositoryImpl implements JpaLoyaltyRepository {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void save(LoyaltyHistory loyaltyHistory) {
        JpaLoyaltyHistoryEntity entity = new JpaLoyaltyHistoryEntity(loyaltyHistory);

        if (entityManager.find(JpaLoyaltyHistoryEntity.class, loyaltyHistory.getId()) == null) {
            entityManager.persist(entity);
        }

        else {
            entityManager.merge(entity);
        }
    }

    @Override
    public List<LoyaltyHistory> findHistoryByRiderId(UUID riderId) {
        return entityManager.createQuery(
                "SELECT h FROM JpaLoyaltyHistoryEntity h WHERE h.riderId = :riderId ORDER BY h.reachedAt DESC",
                JpaLoyaltyHistoryEntity.class).setParameter("riderId", riderId).getResultStream()
                .map(JpaLoyaltyHistoryEntity::toDomain).collect(Collectors.toList());
    }

    @Override
    public LoyaltyTier findCurrentTier(UUID riderId) {
        List<LoyaltyHistory> history = findHistoryByRiderId(riderId);

        if (history.isEmpty()) {
            return LoyaltyTier.ENTRY;
        }

        return history.get(0).getTier();
    }
}
