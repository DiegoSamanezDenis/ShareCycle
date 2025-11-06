package com.sharecycle.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class Bill {
    private UUID billId;
    private LocalDateTime computedAt;
    private double totalCost;
    private double baseCost;
    private double timeCost;
    private double eBikeSurcharge;

    public Bill() {
        this.billId = UUID.randomUUID();
        this.computedAt = LocalDateTime.now();
    }

    public Bill(double baseCost, double timeCost, double eBikeSurcharge) {
        this();
        this.baseCost = baseCost;
        this.timeCost = timeCost;
        this.eBikeSurcharge = eBikeSurcharge;
        this.totalCost = baseCost + timeCost + eBikeSurcharge;
    }

    public Bill(UUID billId,
                LocalDateTime computedAt,
                double baseCost,
                double timeCost,
                double eBikeSurcharge,
                double totalCost) {
        this.billId = billId == null ? UUID.randomUUID() : billId;
        this.computedAt = computedAt == null ? LocalDateTime.now() : computedAt;
        this.baseCost = baseCost;
        this.timeCost = timeCost;
        this.eBikeSurcharge = eBikeSurcharge;
        this.totalCost = totalCost;
    }

    public UUID getBillId() {
        return billId;
    }

    public LocalDateTime getComputedAt() {
        return computedAt;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public double getBaseCost() {
        return baseCost;
    }

    public double getTimeCost() {
        return timeCost;
    }

    public double getEBikeSurcharge() {
        return eBikeSurcharge;
    }
}
