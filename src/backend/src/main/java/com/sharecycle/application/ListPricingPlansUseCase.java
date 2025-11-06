package com.sharecycle.application;

import com.sharecycle.domain.DefaultPricingPlans;
import com.sharecycle.domain.model.PricingPlan;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ListPricingPlansUseCase {

    private static final int SAMPLE_MINUTES = 30;

    public List<PricingPlanSummary> execute() {
        return Arrays.stream(PricingPlan.PlanType.values())
                .map(DefaultPricingPlans::planForType)
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    private PricingPlanSummary toSummary(PricingPlan plan) {
        double standardCost = plan.getBaseCost() + plan.getPerMinuteRate() * SAMPLE_MINUTES;
        double eBikeCost = standardCost;
        if (plan.getEBikeSurchargePerMinute() != null) {
            eBikeCost += plan.getEBikeSurchargePerMinute() * SAMPLE_MINUTES;
        }
        SampleBreakdown sample = new SampleBreakdown(SAMPLE_MINUTES, roundCurrency(standardCost), roundCurrency(eBikeCost));
        Double subscriptionFee = plan.getType() == PricingPlan.PlanType.MONTHLY_SUBSCRIBER
                ? DefaultPricingPlans.monthlySubscriptionFee()
                : null;
        return new PricingPlanSummary(plan, sample, subscriptionFee);
    }

    private double roundCurrency(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public record PricingPlanSummary(PricingPlan plan, SampleBreakdown sample, Double subscriptionFee) { }

    public record SampleBreakdown(int durationMinutes, double standardBikeCost, double eBikeCost) { }
}
