package com.sharecycle.domain.repository;

import com.sharecycle.domain.model.Reservation;

import java.util.List;
import java.util.UUID;

public interface ReservationRepository {
    void save(Reservation reservation);
    boolean existsById(UUID id);
    Reservation findById(UUID id);
    boolean existsByRiderId(UUID riderId);
    Reservation findByRiderId(UUID riderId);
    List<Reservation> findExpiredReservations();

}
