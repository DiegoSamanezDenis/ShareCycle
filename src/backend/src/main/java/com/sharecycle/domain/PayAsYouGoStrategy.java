package com.sharecycle.domain;

import com.sharecycle.domain.model.Bill;
import com.sharecycle.domain.model.PricingPlan;
import com.sharecycle.domain.model.Trip;
import com.sharecycle.domain.repository.PricingStrategyRepository;
public class PayAsYouGoStrategy implements PricingStrategyRepository {

    private static final double PER_MINUTE_RATE = 0.05;
    private static final double EBIKE_SURCHARGE_PER_MINUTE = 0.02;

    @Override
    public Bill calculate(Trip trip, PricingPlan plan) {
        double perMinuteRate = PER_MINUTE_RATE;
        if ("EBIKE".equals(trip.getBike().getType())) {
            perMinuteRate += EBIKE_SURCHARGE_PER_MINUTE;
        }
        double subtotal = trip.getDurationMinutes() * perMinuteRate;
        return null;
    }
    public String displayInfo() {
        return "Pay As You Go Strategy: Per Minute Rate = " + PER_MINUTE_RATE+"\n"+
            "E-Bike Surcharge Per Minute = " + EBIKE_SURCHARGE_PER_MINUTE;
    }
}
