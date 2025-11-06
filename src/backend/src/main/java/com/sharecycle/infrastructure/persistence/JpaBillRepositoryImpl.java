package com.sharecycle.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.sharecycle.domain.model.Bill;
import com.sharecycle.domain.model.User;
import com.sharecycle.domain.repository.JpaBillRepository;
import com.sharecycle.infrastructure.persistence.jpa.JpaLedgerEntryEntity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@Repository
@Transactional
public class JpaBillRepositoryImpl implements JpaBillRepository {

    @PersistenceContext
    private final EntityManager entityManager;

    public JpaBillRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Bill findById(UUID billId) {
        try {
            JpaLedgerEntryEntity entity = entityManager.createQuery(
                            "SELECT l FROM JpaLedgerEntryEntity l WHERE l.billId = :billId",
                            JpaLedgerEntryEntity.class)
                    .setParameter("billId", billId)
                    .getSingleResult();

            return extractBill(entity);
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public List<Bill> findAllByUser(User user) {
        return entityManager.createQuery(
                        "SELECT l FROM JpaLedgerEntryEntity l WHERE l.user.userId = :userId ORDER BY l.billComputedAt DESC",
                        JpaLedgerEntryEntity.class)
                .setParameter("userId", user.getUserId())
                .getResultStream()
                .map(this::extractBill)
                .collect(Collectors.toList());
    }

    @Override
    public List<Bill> findByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return entityManager.createQuery(
                        "SELECT l FROM JpaLedgerEntryEntity l WHERE l.billComputedAt BETWEEN :startDate AND :endDate ORDER BY l.billComputedAt DESC",
                        JpaLedgerEntryEntity.class)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .getResultStream()
                .map(this::extractBill)
                .collect(Collectors.toList());
    }

    @Override
    public List<Bill> findByUserAndDateRange(User user, LocalDateTime startDate, LocalDateTime endDate) {
        return entityManager.createQuery(
                        "SELECT l FROM JpaLedgerEntryEntity l WHERE l.user.userId = :userId " +
                                "AND l.billComputedAt BETWEEN :startDate AND :endDate ORDER BY l.billComputedAt DESC",
                        JpaLedgerEntryEntity.class)
                .setParameter("userId", user.getUserId())
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .getResultStream()
                .map(this::extractBill)
                .collect(Collectors.toList());
    }

    @Override
    public List<Bill> findByPricingPlan(String pricingPlan) {
        return entityManager.createQuery(
                        "SELECT l FROM JpaLedgerEntryEntity l WHERE l.pricingPlan = :pricingPlan ORDER BY l.billComputedAt DESC",
                        JpaLedgerEntryEntity.class)
                .setParameter("pricingPlan", pricingPlan)
                .getResultStream()
                .map(this::extractBill)
                .collect(Collectors.toList());
    }

    @Override
    public Bill findMostRecentByUser(User user) {
        try {
            JpaLedgerEntryEntity entity = entityManager.createQuery(
                            "SELECT l FROM JpaLedgerEntryEntity l WHERE l.user.userId = :userId " +
                                    "ORDER BY l.billComputedAt DESC",
                            JpaLedgerEntryEntity.class)
                    .setParameter("userId", user.getUserId())
                    .setMaxResults(1)
                    .getSingleResult();

            return extractBill(entity);
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     * Helper method to extract Bill from JpaLedgerEntryEntity
     */
    private Bill extractBill(JpaLedgerEntryEntity entity) {
        if (entity == null || entity.getBillId() == null) {
            return null;
        }

        return new Bill(
                entity.getBillId(),
                entity.getBillComputedAt(),
                entity.getBaseCost(),
                entity.getTimeCost(),
                entity.getEBikeSurcharge(),
                entity.getTotalCost()
        );
    }
}
