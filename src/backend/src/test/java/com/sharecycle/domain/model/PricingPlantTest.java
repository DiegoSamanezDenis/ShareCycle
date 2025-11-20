package com.sharecycle.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class PricingPlanTest {

    @Test
    void testGettersSetters() {
        PricingPlan plan = new PricingPlan(UUID.randomUUID(), "Monthly", "Monthly plan", 10.0, 0.5, 0.2, PricingPlan.PlanType.MONTHLY_SUBSCRIBER);
        assertEquals("Monthly", plan.getName());
        assertEquals(PricingPlan.PlanType.MONTHLY_SUBSCRIBER, plan.getType());

        plan.setType(PricingPlan.PlanType.PAY_AS_YOU_GO);
        assertEquals(PricingPlan.PlanType.PAY_AS_YOU_GO, plan.getType());
    }
}
