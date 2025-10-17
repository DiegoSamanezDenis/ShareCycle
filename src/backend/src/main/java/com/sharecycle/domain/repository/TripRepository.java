package com.sharecycle.domain.repository;

import com.sharecycle.model.entity.Reservation;
import com.sharecycle.model.entity.Trip;

import java.util.UUID;

public interface TripRepository {
    void save(Trip trip);
    Trip findById(UUID id);

}
