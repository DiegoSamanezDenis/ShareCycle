package com.sharecycle.infrastructure.persistence.jpa;

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

import java.time.Instant;
import java.util.UUID;

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

    @Column(name = "total_amount", nullable = false)
    private double totalAmount;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    public JpaLedgerEntryEntity() {
    }

    private JpaLedgerEntryEntity(LedgerEntry ledgerEntry, MapperContext context) {
        this.ledgerId = ledgerEntry.getLedgerId();
        this.user = (JpaUserEntity) JpaUserEntity.fromDomain(ledgerEntry.getUser());
        this.trip = JpaTripEntity.fromDomain(ledgerEntry.getTrip(), context);
        this.status = ledgerEntry.getLedgerStatus();
        this.totalAmount = ledgerEntry.getTotalAmount();
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
        LedgerEntry ledgerEntry = new LedgerEntry(
                ledgerId,
                user.toDomain(),
                trip.toDomain(context),
                status,
                totalAmount,
                timestamp
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
}
