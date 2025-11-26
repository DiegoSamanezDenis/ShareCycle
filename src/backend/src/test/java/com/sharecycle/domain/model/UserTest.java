package com.sharecycle.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserTest {

    private User user;

    @BeforeEach
    void setup() {
        user = new User(UUID.randomUUID(), "John Doe", "123 Street", "john@example.com", "john_doe",
                "hash123", "USER", "token123", LocalDateTime.now(), LocalDateTime.now(), 0.0);
    }


    @Test
    void testGettersSetters() {
        user.setFullName("Alice");
        assertEquals("Alice", user.getFullName());

        user.setRole("ADMIN");
        assertEquals("ADMIN", user.getRole());

        user.setPricingPlanType(PricingPlan.PlanType.PAY_AS_YOU_GO);
        assertEquals(PricingPlan.PlanType.PAY_AS_YOU_GO, user.getPricingPlanType());
    }

    @Test
    void testTouchMethods() {
        user.touchOnCreate();
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());

        LocalDateTime beforeUpdate = user.getUpdatedAt();
        user.touchOnUpdate();
        assertTrue(user.getUpdatedAt().isAfter(beforeUpdate) || user.getUpdatedAt().isEqual(beforeUpdate));
    }
}
