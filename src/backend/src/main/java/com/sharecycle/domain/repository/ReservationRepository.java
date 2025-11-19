package com.sharecycle.domain.repository;

import com.sharecycle.domain.model.Reservation;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ReservationRepository {
    void save(Reservation reservation);
    boolean existsById(UUID id);
    Reservation findById(UUID id);
    boolean existsByRiderId(UUID riderId);
    Reservation findByRiderId(UUID riderId);
    List<Reservation> findExpiredReservations();
    boolean hasActiveReservationForBike(UUID bikeId);

    int countReservationsByRiderIdAfter(UUID riderId, Instant since);
}
