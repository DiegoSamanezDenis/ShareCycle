package com.sharecycle.application;

import com.sharecycle.domain.model.Bike;
import com.sharecycle.domain.model.LedgerEntry;
import com.sharecycle.domain.model.Trip;
import com.sharecycle.domain.model.User;
import com.sharecycle.domain.repository.JpaLedgerEntryRepository;
import com.sharecycle.infrastructure.persistence.JpaTripRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ListTripsUseCase {

    private final Logger logger = LoggerFactory.getLogger(ListTripsUseCase.class);

    private final JpaTripRepository jpaTripRepository;
    private final JpaLedgerEntryRepository ledgerEntryRepository;

    public ListTripsUseCase(JpaTripRepository jpaTripRepository,
                            JpaLedgerEntryRepository ledgerEntryRepository) {
        this.jpaTripRepository = jpaTripRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }


    public List<TripHistoryEntry> execute(User user,
                                          LocalDateTime startTime,
                                          LocalDateTime endTime,
                                          Bike.BikeType bikeType) {
        String role = user.getRole();
        List<Trip> trips;
        if ("OPERATOR".equals(role)) {
            logger.info("User is an operator, finding all trips");
            trips = jpaTripRepository.findAllWithFilter(startTime, endTime, bikeType);
        } else {
            logger.info("User is a rider, finding all trips by this rider");
            trips = jpaTripRepository.findAllByUserIdWithFilter(user.getUserId(), startTime, endTime, bikeType);
        }

        if (trips.isEmpty()) {
            return List.of();
        }

        Map<UUID, LedgerEntry> ledgerEntriesByTripId = ledgerEntryRepository.findAllByTripIds(
                        trips.stream()
                                .map(Trip::getTripID)
                                .toList())
                .stream()
                .filter(entry -> entry.getTrip() != null && entry.getTrip().getTripID() != null)
                .collect(Collectors.toMap(entry -> entry.getTrip().getTripID(), Function.identity(), (existing, replacement) -> replacement));

        return trips.stream()
                .map(trip -> toHistoryEntry(trip, ledgerEntriesByTripId.get(trip.getTripID())))
                .toList();
    }

    private TripHistoryEntry toHistoryEntry(Trip trip, LedgerEntry ledgerEntry) {
        var rider = trip.getRider();
        var startStation = trip.getStartStation();
        var endStation = trip.getEndStation();
        double totalCost = ledgerEntry != null && ledgerEntry.getBill() != null
                ? ledgerEntry.getBill().getTotalCost()
                : 0.0;
        UUID ledgerId = ledgerEntry != null ? ledgerEntry.getLedgerId() : null;
        LedgerEntry.LedgerStatus ledgerStatus = ledgerEntry != null ? ledgerEntry.getLedgerStatus() : null;
        return new TripHistoryEntry(
                trip.getTripID(),
                rider != null ? rider.getUserId() : null,
                rider != null && rider.getFullName() != null ? rider.getFullName() : rider != null ? rider.getUsername() : null,
                startStation != null ? startStation.getName() : null,
                endStation != null ? endStation.getName() : null,
                trip.getStartTime(),
                trip.getEndTime(),
                trip.getDurationMinutes(),
                trip.getBike() != null ? trip.getBike().getType() : null,
                totalCost,
                ledgerId,
                ledgerStatus
        );
    }

    public record TripHistoryEntry(
            UUID tripId,
            UUID riderId,
            String riderName,
            String startStationName,
            String endStationName,
            LocalDateTime startTime,
            LocalDateTime endTime,
            int durationMinutes,
            Bike.BikeType bikeType,
            double totalCost,
            UUID ledgerId,
            LedgerEntry.LedgerStatus ledgerStatus
    ) {
    }
}
