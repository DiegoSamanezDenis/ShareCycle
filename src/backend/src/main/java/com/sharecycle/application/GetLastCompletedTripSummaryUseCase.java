package com.sharecycle.application;

import com.sharecycle.domain.model.Bill;
import com.sharecycle.domain.model.LedgerEntry;
import com.sharecycle.domain.model.Trip;
import com.sharecycle.domain.repository.JpaLedgerEntryRepository;
import com.sharecycle.domain.repository.TripRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class GetLastCompletedTripSummaryUseCase {

    private final TripRepository tripRepository;
    private final JpaLedgerEntryRepository ledgerEntryRepository;

    public GetLastCompletedTripSummaryUseCase(TripRepository tripRepository,
                                              JpaLedgerEntryRepository ledgerEntryRepository) {
        this.tripRepository = tripRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    public TripSummary execute(UUID riderId) {
        Trip trip = tripRepository.findMostRecentCompletedByUserId(riderId);
        if (trip == null) {
            return null;
        }
        LedgerEntry ledgerEntry = ledgerEntryRepository.findByTrip(trip);
        Bill bill = ledgerEntry != null ? ledgerEntry.getBill() : null;
        double base = bill != null ? bill.getBaseCost() : 0.0;
        double time = bill != null ? bill.getTimeCost() : 0.0;
        double surcharge = bill != null ? bill.getEBikeSurcharge() : 0.0;
        double total = bill != null ? bill.getTotalCost() : 0.0;
        LedgerEntry.LedgerStatus status = ledgerEntry != null ? ledgerEntry.getLedgerStatus() : null;
        double discountRate = 0.0;
        double discountAmount = 0.0;
        if (ledgerEntry != null && ledgerEntry.getTrip() != null) {
            discountRate = ledgerEntry.getTrip().getAppliedDiscountRate();
        }
        double preDiscountTotal = bill != null ? bill.getBaseCost() + bill.getTimeCost() + bill.getEBikeSurcharge() : 0.0;
        discountAmount = bill != null ? Math.max(0.0, preDiscountTotal - bill.getTotalCost()) : 0.0;
        double flexCreditApplied = bill != null ? bill.getFlexCreditApplied() : 0.0;
        return new TripSummary(
                trip.getTripID(),
                trip.getEndStation() != null ? trip.getEndStation().getId() : null,
                trip.getEndTime(),
                trip.getDurationMinutes(),
                ledgerEntry != null ? ledgerEntry.getLedgerId() : null,
                base,
                time,
                surcharge,
                total,
                status,
                discountRate,
                discountAmount,
                flexCreditApplied
        );
    }

    public record TripSummary(
            UUID tripId,
            UUID endStationId,
            LocalDateTime endedAt,
            int durationMinutes,
            UUID ledgerId,
            double baseCost,
            double timeCost,
            double eBikeSurcharge,
            double totalCost,
            LedgerEntry.LedgerStatus ledgerStatus,
            double discountRate,
            double discountAmount,
            double flexCreditApplied
    ) { }
}
