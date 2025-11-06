package com.sharecycle.domain;

import com.sharecycle.domain.model.Bike;
import com.sharecycle.domain.model.Bill;
import com.sharecycle.domain.model.PricingPlan;
import com.sharecycle.domain.model.Trip;
import com.sharecycle.domain.repository.PricingStrategyRepository;
public class PayAsYouGoStrategy implements PricingStrategyRepository {

    private static final double PER_MINUTE_RATE = 0.05;
    private static final double EBIKE_SURCHARGE_PER_MINUTE = 0.01;

    @Override
    public Bill calculate(Trip trip, PricingPlan plan) {
        int minutes = trip.getDurationMinutes();
        double timeCost = minutes * PER_MINUTE_RATE;
        double eBikeSurcharge = 0.0;
        
        if (trip.getBike().getType() == Bike.BikeType.E_BIKE) {
            eBikeSurcharge = minutes * EBIKE_SURCHARGE_PER_MINUTE;
        }
        
        return new Bill(10, timeCost, eBikeSurcharge);
    }
    public String displayInfo() {
        return "Pay As You Go Strategy: Per Minute Rate = " + PER_MINUTE_RATE+"\n"+
            "E-Bike Surcharge Per Minute = " + EBIKE_SURCHARGE_PER_MINUTE;
    }
}
