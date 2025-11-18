package com.sharecycle.service;

import com.sharecycle.domain.model.LoyaltyTier;
import com.sharecycle.domain.model.Trip;
import com.sharecycle.domain.repository.TripRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class LoyaltyEvaluatorService {
    private final TripRepository tripRepository;

    public LoyaltyEvaluatorService(TripRepository tripRepository) {
        this.tripRepository = tripRepository;
    }

    public LoyaltyTier calculateTier(UUID riderId) {
        List<Trip> trips = tripRepository.findAllByUserId(riderId);
        long completedTrips = trips.stream()
                .filter(trip -> trip.getEndTime() != null)
                .count();
    }
}
