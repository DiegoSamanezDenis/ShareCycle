package com.sharecycle.domain;

import java.util.List;
import java.util.UUID;

import com.sharecycle.domain.model.PricingPlan;

public final class DefaultPricingPlans {

    private static final UUID PAYG_ID = UUID.fromString("be1d5a27-7180-4e57-a6c0-7fa34dd5671f");
    private static final UUID MONTHLY_ID = UUID.fromString("5dd5a123-2fe4-410f-9ad2-2fbfda1a93f1");
    private static final double MONTHLY_SUBSCRIPTION_FEE = 20.0;

    private DefaultPricingPlans() {
    }

    public static List<PricingPlan> defaults() {
        return List.of(
                planForType(PricingPlan.PlanType.PAY_AS_YOU_GO),
                planForType(PricingPlan.PlanType.MONTHLY_SUBSCRIBER)
        );
    }

    public static PricingPlan planForType(PricingPlan.PlanType type) {
        return switch (type) {
            case MONTHLY_SUBSCRIBER -> new PricingPlan(
                    MONTHLY_ID,
                    "Monthly Subscriber",
                    "Unlimited standard and e-bike rides for a flat $20 monthly subscription.",
                    0.0,
                    0.0,
                    0.0,
                    PricingPlan.PlanType.MONTHLY_SUBSCRIBER
            );
            case PAY_AS_YOU_GO -> new PricingPlan(
                    PAYG_ID,
                    "Pay As You Go",
                    "Only pay for what you ride with a fast demo-friendly rate and optional e-bike surcharge.",
                    0.0,
                    6.0,
                    0.60,
                    PricingPlan.PlanType.PAY_AS_YOU_GO
            );
        };
    }

    public static double monthlySubscriptionFee() {
        return MONTHLY_SUBSCRIPTION_FEE;
    }
}
