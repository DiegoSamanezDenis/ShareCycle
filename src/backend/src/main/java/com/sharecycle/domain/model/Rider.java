package com.sharecycle.domain.model;

import java.util.UUID;

public class Rider extends User {

    public Rider() {
        super.setRole("RIDER");
        super.setPricingPlanType(PricingPlan.PlanType.PAY_AS_YOU_GO);
    }

    public Rider(User user) {
        super(user.getUserId(),
                user.getFullName(),
                user.getStreetAddress(),
                user.getEmail(),
                user.getUsername(),
                user.getPasswordHash(),
                "RIDER",
                user.getPaymentMethodToken(),
                user.getCreatedAt(),
                user.getUpdatedAt());
        super.setPricingPlanType(user.getPricingPlanType());
    }

    public Rider(String fullName,
                 String streetAddress,
                 String email,
                 String username,
                 String passwordHash,
                 String paymentMethodToken,
                 PricingPlan.PlanType pricingPlanType) {
        super(UUID.randomUUID(),
                fullName,
                streetAddress,
                email,
                username,
                passwordHash,
                "RIDER",
                paymentMethodToken,
                null,
                null);
        super.setPricingPlanType(pricingPlanType);
    }
}
