package com.sharecycle.infrastructure;

import com.sharecycle.domain.repository.TripRepository;
import com.sharecycle.model.entity.Reservation;
import com.sharecycle.model.entity.Trip;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.UUID;
@Repository
public class JpaTripRepository implements TripRepository {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void save(Trip trip) {
        if (trip.getTripID() == null)
            entityManager.persist(trip);
        else
            entityManager.merge(trip);
    }
    @Override
    public Trip findById(UUID id) {
        return entityManager.find(Trip.class, id);
    }
}
