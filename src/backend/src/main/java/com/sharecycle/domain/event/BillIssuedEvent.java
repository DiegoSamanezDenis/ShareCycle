package com.sharecycle.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record BillIssuedEvent(
        UUID tripId,
        UUID riderId,
        UUID billId,
        UUID ledgerId,
        LocalDateTime computedAt,
        double baseCost,
        double timeCost,
        double eBikeSurcharge,
        double totalCost,
        String pricingPlan
) {}
