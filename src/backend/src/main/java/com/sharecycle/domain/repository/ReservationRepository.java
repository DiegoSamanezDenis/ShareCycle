package com.sharecycle.domain.repository;

import com.sharecycle.model.entity.Reservation;

import java.util.UUID;

public interface ReservationRepository {
    void save(Reservation reservation);
    boolean existsById(UUID id);
    Reservation findById(UUID id);
    boolean existsByRiderId(UUID riderId);
    Reservation findByRiderId(UUID riderId);
}