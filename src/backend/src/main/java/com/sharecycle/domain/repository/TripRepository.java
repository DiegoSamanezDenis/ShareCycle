package com.sharecycle.domain.repository;

import com.sharecycle.domain.model.Bike;
import com.sharecycle.domain.model.Trip;

import java.time.LocalDateTime;
import java.util.List;
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

    List<Trip> findAll();
    Trip findMostRecentCompletedByUserId(UUID userId);

    List<Trip> findAllByUserId(UUID userId);

    List<Trip> findAllWithFilter(LocalDateTime startDate, LocalDateTime endDate, Bike.BikeType bikeType);

    List<Trip> findAllByUserIdWithFilter(UUID userId, LocalDateTime startDate, LocalDateTime endDate, Bike.BikeType bikeType);
}
