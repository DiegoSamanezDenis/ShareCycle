package com.sharecycle.domain.repository;

import com.sharecycle.domain.model.Trip;

import java.util.UUID;

public interface TripRepository {
    void save(Trip trip);
    Trip findById(UUID id);
    void deleteById(UUID id);
    boolean riderHasActiveTrip(UUID riderId);
    Trip findByUserId(UUID userId);
    Trip findByBikeId(UUID bikeId);
    void deleteByUserId(UUID userId);
    void deleteByBikeId(UUID bikeId);
}
