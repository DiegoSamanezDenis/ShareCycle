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
import java.util.Comparator;
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


    public TripHistoryPage execute(User user,
                                   LocalDateTime startTime,
                                   LocalDateTime endTime,
                                   Bike.BikeType bikeType,
                                   int page,
                                   int pageSize,
                                   String tripIdQueryRaw,
                                   String effectiveRoleValue) {
        String role = effectiveRoleValue != null ? effectiveRoleValue : user.getRole();
        int safePage = Math.max(0, page);
        int safePageSize = Math.max(1, pageSize);
        String tripIdQuery = tripIdQueryRaw != null ? tripIdQueryRaw.trim().toLowerCase() : null;
        boolean hasTripIdFilter = tripIdQuery != null && !tripIdQuery.isEmpty();
        List<Trip> trips;
        long totalCount;
        boolean riderRequest = "RIDER".equalsIgnoreCase(role);
        if (hasTripIdFilter) {
            logger.info("Applying tripId filter to trip history search");
            List<Trip> scopedTrips;
            if (!riderRequest) {
                scopedTrips = jpaTripRepository.findAllWithFilter(startTime, endTime, bikeType);
            } else {
                scopedTrips = jpaTripRepository.findAllByUserIdWithFilter(user.getUserId(), startTime, endTime, bikeType);
            }
            List<Trip> filtered = scopedTrips.stream()
                    .filter(trip -> trip.getTripID() != null && trip.getTripID().toString().toLowerCase().contains(tripIdQuery))
                    .toList();
            totalCount = filtered.size();
            int fromIndex = Math.min(safePage * safePageSize, filtered.size());
            int toIndex = Math.min(fromIndex + safePageSize, filtered.size());
            trips = filtered.subList(fromIndex, toIndex);
        } else {
            if (!riderRequest) {
                logger.info("User is an operator, finding all trips");
                trips = jpaTripRepository.findAllWithFilterPaged(startTime, endTime, bikeType, safePage, safePageSize);
                totalCount = jpaTripRepository.countAllWithFilter(startTime, endTime, bikeType);
            } else {
                logger.info("User is a rider, finding all trips by this rider");
                trips = jpaTripRepository.findAllByUserIdWithFilterPaged(user.getUserId(), startTime, endTime, bikeType, safePage, safePageSize);
                totalCount = jpaTripRepository.countAllByUserIdWithFilter(user.getUserId(), startTime, endTime, bikeType);
            }
        }

        List<Trip> orderedTrips = sortTripsLatestFirst(trips);
        List<TripHistoryEntry> entries;
        if (orderedTrips.isEmpty()) {
            entries = List.of();
        } else {
            Map<UUID, LedgerEntry> ledgerEntriesByTripId = ledgerEntryRepository.findAllByTripIds(
                            orderedTrips.stream()
                                    .map(Trip::getTripID)
                                    .toList())
                    .stream()
                    .filter(entry -> entry.getTrip() != null && entry.getTrip().getTripID() != null)
                    .collect(Collectors.toMap(entry -> entry.getTrip().getTripID(), Function.identity(), (existing, replacement) -> replacement));

            boolean includeBilling = true;
            entries = orderedTrips.stream()
                    .map(trip -> toHistoryEntry(trip, ledgerEntriesByTripId.get(trip.getTripID()), includeBilling))
                    .toList();
        }

        int totalPages = safePageSize <= 0 ? 0 : (int) Math.ceil(totalCount / (double) safePageSize);
        boolean hasNext = safePage < totalPages - 1;
        boolean hasPrevious = safePage > 0;
        return new TripHistoryPage(entries, safePage, safePageSize, totalCount, totalPages, hasNext, hasPrevious);
    }

    private List<Trip> sortTripsLatestFirst(List<Trip> trips) {
        if (trips == null || trips.isEmpty()) {
            return List.of();
        }
        Comparator<Trip> comparator = Comparator
                .comparing(
                        Trip::getEndTime,
                        Comparator.nullsLast(Comparator.reverseOrder())
                )
                .thenComparing(
                        Trip::getStartTime,
                        Comparator.nullsLast(Comparator.reverseOrder())
                );
        return trips.stream()
                .sorted(comparator)
                .toList();
    }

    private TripHistoryEntry toHistoryEntry(Trip trip, LedgerEntry ledgerEntry, boolean includeBilling) {
        var rider = trip.getRider();
        var startStation = trip.getStartStation();
        var endStation = trip.getEndStation();
        double totalCost = includeBilling && ledgerEntry != null && ledgerEntry.getBill() != null
                ? ledgerEntry.getBill().getTotalCost()
                : 0.0;
        UUID ledgerId = includeBilling && ledgerEntry != null ? ledgerEntry.getLedgerId() : null;
        LedgerEntry.LedgerStatus ledgerStatus = includeBilling && ledgerEntry != null ? ledgerEntry.getLedgerStatus() : null;
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
                trip.getBike() != null ? trip.getBike().getId() : null,
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
            UUID bikeId,
            double totalCost,
            UUID ledgerId,
            LedgerEntry.LedgerStatus ledgerStatus
    ) {
    }

    public record TripHistoryPage(
            List<TripHistoryEntry> entries,
            int page,
            int pageSize,
            long totalItems,
            int totalPages,
            boolean hasNext,
            boolean hasPrevious
    ) {
    }
}
