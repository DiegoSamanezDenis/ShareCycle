package com.sharecycle.model.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
public class LedgerEntry {
    public enum LedgerStatus {
        PENDING, PAID
    }

    @Id
    @Column(name = "ledger_id", unique = true, nullable = false)
    private UUID ledgerId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = false)
    @JoinColumn(name = "trip_id")
    private Trip trip;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status", nullable = false)
    private LedgerStatus ledgerStatus;

    @Column(name = "total_amount", nullable = false)
    private double totalAmount;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    public LedgerEntry() {}

    public LedgerEntry(Trip trip) {
        this.ledgerId = UUID.randomUUID();
        this.user = trip.getRider();
        this.trip = trip;
        this.ledgerStatus = LedgerStatus.PENDING;
        this.timestamp = Instant.now();
        this.generateBill();
    }

    public Bill generateBill() {
        Bill bill = new Bill(this.trip);
        this.totalAmount = bill.getTotal();
        return bill;
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
