package com.sharecycle.application;

import com.sharecycle.domain.model.Bill;
import com.sharecycle.domain.model.LedgerEntry;
import com.sharecycle.domain.model.Trip;
import com.sharecycle.domain.model.User;
import com.sharecycle.infrastructure.persistence.JpaTripRepository;
import com.sharecycle.domain.repository.JpaLedgerEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import java.time.LocalDateTime;

@Service
public class GetTripDetailsUseCase {

    private final Logger logger = LoggerFactory.getLogger(GetTripDetailsUseCase.class);
    private final JpaTripRepository tripRepository;
    private final JpaLedgerEntryRepository ledgerEntryRepository;

    public GetTripDetailsUseCase(JpaTripRepository tripRepository, JpaLedgerEntryRepository ledgerEntryRepository) {
        this.tripRepository = tripRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    public TripDetails execute(UUID tripId, User requestingUser) {
        Trip trip = tripRepository.findById(tripId);
        if (trip == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found");
        }
        boolean isOperator = "OPERATOR".equals(requestingUser.getRole());
        boolean isOwner = trip.getRider() != null && trip.getRider().getUserId() != null && trip.getRider().getUserId().equals(requestingUser.getUserId());
        if (!isOperator && !isOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        LedgerEntry ledgerEntry = ledgerEntryRepository.findByTrip(trip);
        Bill bill = ledgerEntry != null ? ledgerEntry.generateBill() : new Bill(trip);
        String riderName = trip.getRider() != null ? trip.getRider().getFullName() : null;
        String startStationName = trip.getStartStation() != null ? trip.getStartStation().getName() : null;
        String endStationName = trip.getEndStation() != null ? trip.getEndStation().getName() : null;
        UUID ledgerID = ledgerEntry != null ? ledgerEntry.getLedgerId() : null;
        double totalCost = ledgerEntry != null ? ledgerEntry.getTotalAmount() : bill.getTotal();
        return new TripDetails(
            trip.getTripID(),
            trip.getRider() != null ? trip.getRider().getUserId() : null,
            riderName,
            startStationName,
            endStationName,
            trip.getStartTime(),
            trip.getEndTime(),
            trip.getDurationMinutes(),
            trip.getBike() != null ? trip.getBike().getType().name() : null,
            totalCost,
            bill.getSubtotal(),
            bill.getTaxAmount(),
            ledgerID,
            ledgerEntry != null ? ledgerEntry.getLedgerStatus() : null

        );
    }

    public record TripDetails(
        UUID tripId,
        UUID riderId,
        String riderName,
        String startStationName,
        String endStationName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        int durationMinutes,
        String bikeType,
        double totalCost,
        double subtotal,
        double taxAmount,
        UUID ledgerId,
        LedgerEntry.LedgerStatus ledgerStatus
    ) {}
    
}
