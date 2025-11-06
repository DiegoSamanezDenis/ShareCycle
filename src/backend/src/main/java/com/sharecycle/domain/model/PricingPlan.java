package com.sharecycle.domain.model;

import java.util.UUID;

public class PricingPlan {
    private UUID planId;
    private String name;
    private double baseFee;
    private PlanType type;
    static double PER_MINUTE_RATE = 0.05;
    static double EBIKE_SURCHARGE_PER_MINUTE = 0.01;

    public enum PlanType {
        PAY_AS_YOU_GO,
        MONTHLY_SUBSCRIBER
    }

    public PricingPlan(UUID planId, String name, double baseFee, PlanType type) {
        this.planId = planId;
        this.name = name;
        this.baseFee = baseFee;
        this.type = type;
    }

    public UUID getPlanId() {
        return planId;
    }

    public String getName() {
        return name;
    }

    public double getBaseFee() {
        return baseFee;
    }

    public PlanType getType() {
        return type;
    }

    public void setType(PlanType type) {
        this.type = type;
    }
}
