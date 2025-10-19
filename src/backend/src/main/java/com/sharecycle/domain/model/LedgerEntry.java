package com.sharecycle.domain.model;

import java.time.Instant;
import java.util.UUID;

public class LedgerEntry {
    public enum LedgerStatus {
        PENDING, PAID
    }

    private UUID ledgerId;
    private User user;
    private Trip trip;
    private LedgerStatus ledgerStatus;
    private double totalAmount;
    private Instant timestamp;

    public LedgerEntry() {
    }

    public LedgerEntry(UUID ledgerId,
                       User user,
                       Trip trip,
                       LedgerStatus status,
                       double totalAmount,
                       Instant timestamp) {
        this.ledgerId = ledgerId == null ? UUID.randomUUID() : ledgerId;
        this.user = user;
        this.trip = trip;
        this.ledgerStatus = status;
        this.totalAmount = totalAmount;
        this.timestamp = timestamp;
    }

    public LedgerEntry(Trip trip) {
        this(UUID.randomUUID(), trip.getRider(), trip, LedgerStatus.PENDING, 0.0, Instant.now());
        Bill bill = generateBill();
        this.totalAmount = bill.getTotal();
    }

    public Bill generateBill() {
        return new Bill(this.trip);
    }

    public UUID getLedgerId() {
        return ledgerId;
    }

    public void setLedgerId(UUID ledgerId) {
        this.ledgerId = ledgerId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Trip getTrip() {
        return trip;
    }

    public void setTrip(Trip trip) {
        this.trip = trip;
    }

    public LedgerStatus getLedgerStatus() {
        return ledgerStatus;
    }

    public void setLedgerStatus(LedgerStatus ledgerStatus) {
        this.ledgerStatus = ledgerStatus;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
