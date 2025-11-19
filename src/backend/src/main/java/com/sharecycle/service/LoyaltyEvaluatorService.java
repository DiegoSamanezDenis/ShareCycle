package com.sharecycle.service;

import com.sharecycle.domain.model.LoyaltyTier;
import com.sharecycle.domain.model.Trip;
import com.sharecycle.domain.repository.ReservationRepository;
import com.sharecycle.domain.repository.TripRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.time.Instant;
import java.time.ZoneOffset;

@Service
public class LoyaltyEvaluatorService {
    private final TripRepository tripRepository;
    private final ReservationRepository reservationRepository;

    public LoyaltyEvaluatorService(TripRepository tripRepository, ReservationRepository reservationRepository) {
        this.tripRepository = tripRepository;
        this.reservationRepository = reservationRepository;
    }

    public EvaluationResult evaluate(UUID riderId, LoyaltyTier currentTier) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneYearAgo = now.minusYears(1);
        LocalDateTime threeMonthsAgo = now.minusMonths(3);

        // Fetch data
        List<Trip> trips = tripRepository.findAllByUserId(riderId);

        // Filter lists
        List<Trip> tripsLastYear = trips.stream()
                .filter(t -> t.getEndTime() != null && t.getEndTime().isAfter(oneYearAgo))
                .toList();

        // BRONZE criteria
        boolean allBikesReturned = trips.stream().noneMatch(t -> t.getEndTime() == null && t.getStartTime().isBefore(now.minusHours(24)));

        boolean brTrips = tripsLastYear.size() >= 10;

        if (!allBikesReturned) {
            return new EvaluationResult(LoyaltyTier.ENTRY, "Outstanding unreturned bike.");
        }

        if (!brTrips) {
            return new EvaluationResult(LoyaltyTier.ENTRY, "Insufficient trips done over last year for BRONZE tier.");
        }

        // SILVER criteria
        Instant oneYearAgoInstant = oneYearAgo.toInstant(ZoneOffset.UTC);
        int recentReservations = reservationRepository.countReservationsByRiderIdAfter(riderId, oneYearAgoInstant);
        boolean silver_Reservations = recentReservations >= 5;
        boolean silver_Frequency = checkMonthlyFrequency(trips, threeMonthsAgo, 5);

        // GOLD criteria
        boolean gold_Frequency = checkWeeklyFrequency(trips, threeMonthsAgo, 5);

        if (gold_Frequency && silver_Frequency && silver_Reservations) {
            return new EvaluationResult(LoyaltyTier.GOLD, "Gold Status Achieved! High trip frequency maintained.");

        }

        if (silver_Frequency && silver_Reservations) {
            return new EvaluationResult(LoyaltyTier.SILVER, "Silver Status Achieved! Consistent monthly trip frequency.");
        }

        // If we met Bronze criteria but failed Silver/Gold checks
        return new EvaluationResult(LoyaltyTier.BRONZE, "Bronze status verified.");
    }

    private boolean checkWeeklyFrequency(List<Trip> trips, LocalDateTime since, int threshold) {
        long totalLast3Months = trips.stream().filter(t -> t.getEndTime() != null && t.getEndTime().isAfter(since)).count();

        long weeks = ChronoUnit.WEEKS.between(since, LocalDateTime.now());
        if (weeks <= 0) {
            weeks = 1;
        }

        double averagePerWeek = (double) totalLast3Months / weeks;

        return averagePerWeek > threshold;
    }

    private boolean checkMonthlyFrequency(List<Trip> trips, LocalDateTime since, int threshold) {
        Map<String, Long> counts = trips.stream().filter(t -> t.getEndTime() != null && t.getEndTime().isAfter(since))
                .collect(Collectors.groupingBy(t -> t.getEndTime().getMonth().toString(), Collectors.counting()));

        if (counts.isEmpty()) {
            return false;
        }

        if (counts.size() < 3) {
            return false;
        }

        return counts.values().stream().allMatch(count -> count > threshold);
    }

    public record EvaluationResult(LoyaltyTier tier, String reason) {}
}
