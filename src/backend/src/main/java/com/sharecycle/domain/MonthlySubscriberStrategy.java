package com.sharecycle.domain;

import com.sharecycle.domain.model.Bill;
import com.sharecycle.domain.model.PricingPlan;
import com.sharecycle.domain.model.Trip;
import com.sharecycle.domain.repository.PricingStrategyRepository;

public class MonthlySubscriberStrategy implements PricingStrategyRepository {

    private static final double DEFAULT_MONTHLY_FEE = DefaultPricingPlans.monthlySubscriptionFee();

    @Override
    public Bill calculate(Trip trip, PricingPlan plan) {
        // Monthly subscribers are not charged per trip; the monthly fee is handled separately.
        return new Bill(0.0, 0.0, 0.0);
    }

    public String displayInfo() {
        return "Monthly Subscriber Strategy: Unlimited rides for $" + DEFAULT_MONTHLY_FEE + " per month (no per-trip fees).";
    }
}
