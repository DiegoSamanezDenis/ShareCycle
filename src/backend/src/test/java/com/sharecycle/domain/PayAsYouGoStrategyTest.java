package com.sharecycle.domain;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sharecycle.domain.model.Bike;
import com.sharecycle.domain.model.Bill;
import com.sharecycle.domain.model.PricingPlan;
import com.sharecycle.domain.model.Rider;
import com.sharecycle.domain.model.Station;
import com.sharecycle.domain.model.Trip;

class PayAsYouGoStrategyTest {

    private final PayAsYouGoStrategy strategy = new PayAsYouGoStrategy();
    private final PricingPlan plan = DefaultPricingPlans.planForType(PricingPlan.PlanType.PAY_AS_YOU_GO);

    @Test
    void standardRideUsesBaseAndTimeCostOnly() {
        Trip trip = buildTrip(Bike.BikeType.STANDARD, "RIDER", 12);

        Bill bill = strategy.calculate(trip, plan, 0.0);

        assertThat(bill.getBaseCost()).isZero();
        assertThat(bill.getTimeCost()).isEqualTo(12 * plan.getPerMinuteRate());
        assertThat(bill.getEBikeSurcharge()).isZero();
        assertThat(bill.getTotalCost()).isEqualTo(bill.getTimeCost());
    }

    @Test
    void eBikeRideAddsSurchargeComponent() {
        Trip trip = buildTrip(Bike.BikeType.E_BIKE, "RIDER", 7);

        Bill bill = strategy.calculate(trip, plan, 0.0);

        assertThat(bill.getTimeCost()).isEqualTo(7 * plan.getPerMinuteRate());
        assertThat(bill.getEBikeSurcharge()).isCloseTo(7 * plan.getEBikeSurchargePerMinute(), within(1e-9));
        assertThat(bill.getTotalCost()).isEqualTo(bill.getTimeCost() + bill.getEBikeSurcharge());
    }

    @Test
    void loyaltyDiscountStacksWithOperatorPerks() {
        Trip trip = buildTrip(Bike.BikeType.E_BIKE, "OPERATOR", 10);

        Bill bill = strategy.calculate(trip, plan, 0.10); // 10% loyalty discount

        double operatorMultiplier = 1 - 0.20; // 20% operator discount
        double loyaltyMultiplier = 0.90;
        double combinedMultiplier = operatorMultiplier * loyaltyMultiplier;

        assertThat(bill.getTimeCost()).isCloseTo(10 * plan.getPerMinuteRate() * combinedMultiplier, within(1e-9));
        assertThat(bill.getEBikeSurcharge()).isCloseTo(10 * plan.getEBikeSurchargePerMinute() * combinedMultiplier, within(1e-9));
        assertThat(bill.getBaseCost()).isZero();
        assertThat(bill.getTotalCost()).isEqualTo(bill.getBaseCost() + bill.getTimeCost() + bill.getEBikeSurcharge());
    }

    private Trip buildTrip(Bike.BikeType bikeType, String riderRole, int minutes) {
        LocalDateTime start = LocalDateTime.of(2024, 1, 1, 12, 0);
        LocalDateTime end = start.plusMinutes(minutes);

        Rider rider = new Rider();
        rider.setUserId(UUID.randomUUID());
        rider.setRole(riderRole);
        rider.setPricingPlanType(PricingPlan.PlanType.PAY_AS_YOU_GO);

        Bike bike = new Bike();
        bike.setId(UUID.randomUUID());
        bike.setType(bikeType);
        bike.setStatus(Bike.BikeStatus.ON_TRIP);

        Station startStation = new Station();
        startStation.setId(UUID.randomUUID());
        startStation.addEmptyDocks(2);
        startStation.markActive();

        Station endStation = new Station();
        endStation.setId(UUID.randomUUID());
        endStation.addEmptyDocks(2);
        endStation.markActive();

        return new Trip(UUID.randomUUID(), start, end, rider, bike, startStation, endStation);
    }
}
