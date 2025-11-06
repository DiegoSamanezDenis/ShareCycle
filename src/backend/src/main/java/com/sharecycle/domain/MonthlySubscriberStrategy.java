package com.sharecycle.domain;
import com.sharecycle.domain.model.Bike;
import com.sharecycle.domain.model.Bill;
import com.sharecycle.domain.model.PricingPlan;
import com.sharecycle.domain.model.Trip;
import com.sharecycle.domain.repository.PricingStrategyRepository;
public class MonthlySubscriberStrategy implements PricingStrategyRepository {

    private static final double MONTHLY_FEE = 100.0;
    private static final double EBIKE_SURCHARGE_PER_MINUTE = 0.01;

    @Override
    public Bill calculate(Trip trip, PricingPlan plan) {
        int minutes = trip.getDurationMinutes();
        double baseCost = MONTHLY_FEE;
        double eBikeSurcharge = 0.0;
        
        if (trip.getBike().getType() == Bike.BikeType.E_BIKE) {
            eBikeSurcharge = minutes * EBIKE_SURCHARGE_PER_MINUTE;
        }
        
        return new Bill(baseCost, 0, eBikeSurcharge);
    }
    public String displayInfo() {
        return "Monthly Subscriber Strategy: Monthly Fee = " + MONTHLY_FEE+"\n"+
            "E-Bike Surcharge Per Minute = " + EBIKE_SURCHARGE_PER_MINUTE;
    }
    
}
