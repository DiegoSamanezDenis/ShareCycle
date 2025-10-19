package com.sharecycle.domain.repository;

import com.sharecycle.domain.model.Reservation;
import com.sharecycle.domain.model.Trip;

import java.util.UUID;

public interface TripRepository {
    void save(Trip trip);
    Trip findById(UUID id);

}
