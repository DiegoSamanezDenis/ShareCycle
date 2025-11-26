package com.sharecycle.infrastructure.persistence.jpa;

import java.time.LocalDateTime;
import java.util.UUID;

import com.sharecycle.domain.model.Bill;
import com.sharecycle.domain.model.LedgerEntry;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "ledger_entry")
public class JpaLedgerEntryEntity {

    @Id
    @Column(name = "ledger_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID ledgerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private JpaUserEntity user;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id")
    private JpaTripEntity trip;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status", nullable = false)
    private LedgerEntry.LedgerStatus status;

    @Column(name = "pricing_plan", length = 50)
    private String pricingPlan;

    @Column(name = "description", length = 255)
    private String description;

    // Bill snapshot fields
    @Column(name = "bill_id", columnDefinition = "BINARY(16)")
    private UUID billId;

    @Column(name = "bill_computed_at")
    private LocalDateTime billComputedAt;

    @Column(name = "base_cost")
    private double baseCost;

    @Column(name = "time_cost")
    private double timeCost;

    @Column(name = "ebike_surcharge")
    private double eBikeSurcharge;

    @Column(name = "total_cost")
    private double totalCost;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "flex_credit_applied")
    private double flexCreditApplied;

    public JpaLedgerEntryEntity() {
    }

    private JpaLedgerEntryEntity(LedgerEntry ledgerEntry, MapperContext context) {
        this.ledgerId = ledgerEntry.getLedgerId();
        this.user = JpaUserEntity.fromDomain(ledgerEntry.getUser());
        if (ledgerEntry.getTrip() != null) {
            this.trip = JpaTripEntity.fromDomain(ledgerEntry.getTrip(), context);
        }
        this.status = ledgerEntry.getLedgerStatus();
        this.pricingPlan = ledgerEntry.getPricingPlan();
        this.description = ledgerEntry.getDescription();
        Bill bill = ledgerEntry.getBill();
        if (bill != null) {
            this.billId = bill.getBillId();
            this.billComputedAt = bill.getComputedAt();
            this.baseCost = bill.getBaseCost();
            this.timeCost = bill.getTimeCost();
            this.eBikeSurcharge = bill.getEBikeSurcharge();
            this.totalCost = bill.getTotalCost();
            this.flexCreditApplied = bill.getFlexCreditApplied();
        }
        this.timestamp = ledgerEntry.getTimestamp();
    }

    public static JpaLedgerEntryEntity fromDomain(LedgerEntry ledgerEntry, MapperContext context) {
        JpaLedgerEntryEntity existing = context.ledgerEntities.get(ledgerEntry.getLedgerId());
        if (existing != null) {
            return existing;
        }
        context.ledgers.put(ledgerEntry.getLedgerId(), ledgerEntry);
        JpaLedgerEntryEntity entity = new JpaLedgerEntryEntity(ledgerEntry, context);
        context.ledgerEntities.put(ledgerEntry.getLedgerId(), entity);
        return entity;
    }

    public LedgerEntry toDomain(MapperContext context) {
        LedgerEntry existing = context.ledgers.get(ledgerId);
        if (existing != null) {
            return existing;
        }
    Bill bill = new Bill(
        billId,
        billComputedAt,
        baseCost,
        timeCost,
        eBikeSurcharge,
        totalCost,
        flexCreditApplied
    );
        LedgerEntry ledgerEntry = new LedgerEntry(
                ledgerId,
                user.toDomain(),
                trip != null ? trip.toDomain(context) : null,
                bill,
                status,
                timestamp,
                pricingPlan,
                description
        );
        context.ledgers.put(ledgerId, ledgerEntry);
        return ledgerEntry;
    }

    public UUID getLedgerId() {
        return ledgerId;
    }

    public void setLedgerId(UUID ledgerId) {
        this.ledgerId = ledgerId;
    }

    public UUID getBillId() {
        return billId;
    }

    public LocalDateTime getBillComputedAt() {
        return billComputedAt;
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

    public double getTotalCost() {
        return totalCost;
    }

    public String getPricingPlan() {
        return pricingPlan;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public double getFlexCreditApplied() {
        return flexCreditApplied;
    }

    public void setFlexCreditApplied(double flexCreditApplied) {
        this.flexCreditApplied = flexCreditApplied;
    }
}
