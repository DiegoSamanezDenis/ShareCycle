package com.sharecycle.domain.model;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.UUID;

class RiderTest {

    @Test
    void testConstructors() {
        User baseUser = new User();
        baseUser.setUserId(UUID.randomUUID());
        baseUser.setRole("RIDER");
        baseUser.setFullName("Base User");
        baseUser.setStreetAddress("123 Base St");
        baseUser.setEmail("base@example.com");
        baseUser.setUsername("baseuser");
        baseUser.setPasswordHash("hash");
        baseUser.setPaymentMethodToken("token");
        baseUser.setPricingPlanType(PricingPlan.PlanType.PAY_AS_YOU_GO);

        // Test Default Constructor
        Rider r1 = new Rider();
        assertEquals("RIDER", r1.getRole());

        // Test Wrapper Constructor
        Rider r2 = new Rider(baseUser);
        assertEquals("RIDER", r2.getRole());
        assertEquals("Base User", r2.getFullName());

        // Test Full Constructor
        Rider r3 = new Rider("Name", "Addr", "email", "username", "hash", "token", PricingPlan.PlanType.MONTHLY_SUBSCRIBER);
        assertEquals("RIDER", r3.getRole());
        assertEquals(PricingPlan.PlanType.MONTHLY_SUBSCRIBER, r3.getPricingPlanType());
    }
}