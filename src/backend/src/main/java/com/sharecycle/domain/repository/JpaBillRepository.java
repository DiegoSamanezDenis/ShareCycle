package com.sharecycle.domain.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.sharecycle.domain.model.Bill;
import com.sharecycle.domain.model.User;

public interface JpaBillRepository {
    /**
     * Find a bill by its unique identifier
     */
    Bill findById(UUID billId);

    /**
     * Find all bills for a specific user
     */
    List<Bill> findAllByUser(User user);

    /**
     * Find bills within a date range
     */
    List<Bill> findByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find bills for a user within a date range
     */
    List<Bill> findByUserAndDateRange(User user, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find bills by pricing plan
     */
    List<Bill> findByPricingPlan(String pricingPlan);

    /**
     * Find the most recent bill for a user
     */
    Bill findMostRecentByUser(User user);
}
