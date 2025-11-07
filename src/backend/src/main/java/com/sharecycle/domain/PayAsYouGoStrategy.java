package com.sharecycle.domain;

import com.sharecycle.domain.model.Bike;
import com.sharecycle.domain.model.Bill;
import com.sharecycle.domain.model.PricingPlan;
import com.sharecycle.domain.model.Trip;
import com.sharecycle.domain.repository.PricingStrategyRepository;
public class PayAsYouGoStrategy implements PricingStrategyRepository {
    private static final double DEFAULT_BASE_COST = 0.0;
    private static final double DEFAULT_PER_MINUTE_RATE = 6.0; // $0.10 per second demo rate
    private static final double DEFAULT_EBIKE_SURCHARGE_PER_MINUTE = 0.60; // $0.01 per second

    @Override
    public Bill calculate(Trip trip, PricingPlan plan) {
        int minutes = trip.getDurationMinutes();
        double perMinuteRate = plan != null ? plan.getPerMinuteRate() : DEFAULT_PER_MINUTE_RATE;
        double baseCost = plan != null ? plan.getBaseCost() : DEFAULT_BASE_COST;
        Double planSurcharge = plan != null ? plan.getEBikeSurchargePerMinute() : null;
        double eBikeSurchargeRate = planSurcharge != null ? planSurcharge : DEFAULT_EBIKE_SURCHARGE_PER_MINUTE;

        double timeCost = minutes * perMinuteRate;
        double eBikeSurcharge = 0.0;

        if (trip.getBike().getType() == Bike.BikeType.E_BIKE) {
            eBikeSurcharge = minutes * eBikeSurchargeRate;
        }

        return new Bill(baseCost, timeCost, eBikeSurcharge);
    }

    public String displayInfo() {
        return "Pay As You Go Strategy: Per Minute Rate = " + DEFAULT_PER_MINUTE_RATE+"\n"+
            "E-Bike Surcharge Per Minute = " + DEFAULT_EBIKE_SURCHARGE_PER_MINUTE;
    }
}
