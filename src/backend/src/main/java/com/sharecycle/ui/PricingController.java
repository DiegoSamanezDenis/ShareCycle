package com.sharecycle.ui;

import com.sharecycle.application.ListPricingPlansUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/pricing")
public class PricingController {

    private final ListPricingPlansUseCase listPricingPlansUseCase;

    public PricingController(ListPricingPlansUseCase listPricingPlansUseCase) {
        this.listPricingPlansUseCase = listPricingPlansUseCase;
    }

    @GetMapping
    public List<PricingPlanResponse> listPricingPlans() {
        return listPricingPlansUseCase.execute().stream()
                .map(PricingPlanResponse::fromDomain)
                .toList();
    }

    public record PricingPlanResponse(
            UUID planId,
            String name,
            String description,
            String planType,
            double baseCost,
            double perMinuteRate,
            Double eBikeSurchargePerMinute,
            Double subscriptionFee,
            SampleExample sample
    ) {
        static PricingPlanResponse fromDomain(ListPricingPlansUseCase.PricingPlanSummary summary) {
            var plan = summary.plan();
            return new PricingPlanResponse(
                    plan.getPlanId(),
                    plan.getName(),
                    plan.getDescription(),
                    plan.getType().name(),
                    plan.getBaseCost(),
                    plan.getPerMinuteRate(),
                    plan.getEBikeSurchargePerMinute(),
                    summary.subscriptionFee(),
                    new SampleExample(
                            summary.sample().durationMinutes(),
                            summary.sample().standardBikeCost(),
                            summary.sample().eBikeCost()
                    )
            );
        }
    }

    public record SampleExample(
            int durationMinutes,
            double standardBikeCost,
            double eBikeCost
    ) { }
}
