package com.sharecycle.domain.repository;

import com.sharecycle.domain.model.Bill;
import com.sharecycle.domain.model.PricingPlan;
import com.sharecycle.domain.model.Trip;

public interface PricingStrategyRepository {
    Bill calculate(Trip trip,PricingPlan plan, double discountRate);
    String displayInfo();
}
