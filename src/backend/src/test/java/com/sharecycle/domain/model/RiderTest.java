package com.sharecycle.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RiderTest {

    @Test
    void testConstructors() {
        User baseUser = new User();

        Rider r1 = new Rider();
        assertEquals("RIDER", r1.getRole());
        assertEquals(PricingPlan.PlanType.PAY_AS_YOU_GO, r1.getPricingPlanType());

        Rider r2 = new Rider(baseUser);
        assertEquals("RIDER", r2.getRole());

        Rider r3 = new Rider("Name", "Addr", "email", "username", "hash", "token", PricingPlan.PlanType.MONTHLY_SUBSCRIBER);
        assertEquals("RIDER", r3.getRole());
        assertEquals(PricingPlan.PlanType.MONTHLY_SUBSCRIBER, r3.getPricingPlanType());
    }
}
