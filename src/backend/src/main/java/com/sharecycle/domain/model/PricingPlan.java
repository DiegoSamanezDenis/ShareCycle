package com.sharecycle.domain.model;

import java.util.UUID;

public class PricingPlan {
    private final UUID planId;
    private final String name;
    private final String description;
    private final double baseCost;
    private final double perMinuteRate;
    private final Double eBikeSurchargePerMinute;
    private PlanType type;

    public enum PlanType {
        PAY_AS_YOU_GO,
        MONTHLY_SUBSCRIBER
    }

    public PricingPlan(UUID planId,
                       String name,
                       String description,
                       double baseCost,
                       double perMinuteRate,
                       Double eBikeSurchargePerMinute,
                       PlanType type) {
        this.planId = planId;
        this.name = name;
        this.description = description;
        this.baseCost = baseCost;
        this.perMinuteRate = perMinuteRate;
        this.eBikeSurchargePerMinute = eBikeSurchargePerMinute;
        this.type = type;
    }

    public UUID getPlanId() {
        return planId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public double getBaseCost() {
        return baseCost;
    }

    public double getPerMinuteRate() {
        return perMinuteRate;
    }

    public Double getEBikeSurchargePerMinute() {
        return eBikeSurchargePerMinute;
    }

    public PlanType getType() {
        return type;
    }

    public void setType(PlanType type) {
        this.type = type;
    }
}
