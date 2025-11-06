package com.sharecycle.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class LedgerEntry {
    public enum LedgerStatus {
        PENDING, PAID
    }

    private UUID ledgerId;
    private User user;
    private Trip trip;
    private Bill bill;
    private LedgerStatus ledgerStatus;
    private LocalDateTime timestamp;
    private String pricingPlan;

    public LedgerEntry() {
        this(UUID.randomUUID(), null, null, null, LedgerStatus.PENDING, LocalDateTime.now(), null);
    }

    public LedgerEntry(User user, Trip trip, Bill bill, String pricingPlan) {
        this(UUID.randomUUID(), user, trip, bill, LedgerStatus.PENDING, LocalDateTime.now(), pricingPlan);
    }

    public LedgerEntry(UUID ledgerId,
                       User user,
                       Trip trip,
                       Bill bill,
                       LedgerStatus status,
                       LocalDateTime timestamp,
                       String pricingPlan) {
        this.ledgerId = ledgerId == null ? UUID.randomUUID() : ledgerId;
        this.user = user;
        this.trip = trip;
        this.bill = bill;
        this.ledgerStatus = status == null ? LedgerStatus.PENDING : status;
        this.timestamp = timestamp == null ? LocalDateTime.now() : timestamp;
        this.pricingPlan = pricingPlan;
    }

    public UUID getLedgerId() {
        return ledgerId;
    }

    public User getUser() {
        return user;
    }

    public Trip getTrip() {
        return trip;
    }

    public Bill getBill() {
        return bill;
    }

    public LedgerStatus getLedgerStatus() {
        return ledgerStatus;
    }

    public void setLedgerStatus(LedgerStatus ledgerStatus) {
        this.ledgerStatus = ledgerStatus;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getPricingPlan() {
        return pricingPlan;
    }

    public void markAsPaid() {
        this.ledgerStatus = LedgerStatus.PAID;
    }
}
