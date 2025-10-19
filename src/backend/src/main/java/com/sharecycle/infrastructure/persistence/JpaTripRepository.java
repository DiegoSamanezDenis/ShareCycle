package com.sharecycle.infrastructure.persistence;

import com.sharecycle.domain.model.Trip;
import com.sharecycle.domain.repository.TripRepository;
import com.sharecycle.infrastructure.persistence.jpa.JpaTripEntity;
import com.sharecycle.infrastructure.persistence.jpa.MapperContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@Transactional
public class JpaTripRepository implements TripRepository {

    @PersistenceContext
    private final EntityManager entityManager;

    public JpaTripRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void save(Trip trip) {
        MapperContext context = new MapperContext();
        JpaTripEntity entity = JpaTripEntity.fromDomain(trip, context);
        if (entity.getTripId() == null) {
            entityManager.persist(entity);
        } else {
            entityManager.merge(entity);
        }
    }

    @Override
    public Trip findById(UUID id) {
        JpaTripEntity entity = entityManager.find(JpaTripEntity.class, id);
        return entity != null ? entity.toDomain(new MapperContext()) : null;
    }
}
