package com.sharecycle.infrastructure.persistence;

import com.sharecycle.domain.model.Reservation;
import com.sharecycle.domain.repository.ReservationRepository;
import com.sharecycle.infrastructure.persistence.jpa.JpaReservationEntity;
import com.sharecycle.infrastructure.persistence.jpa.MapperContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@Transactional
public class JpaReservationRepository implements ReservationRepository {

    @PersistenceContext
    private final EntityManager entityManager;

    public JpaReservationRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void save(Reservation reservation) {
        MapperContext context = new MapperContext();
        JpaReservationEntity entity = JpaReservationEntity.fromDomain(reservation, context);
        if (entity.getReservationId() == null) {
            entityManager.persist(entity);
        } else {
            entityManager.merge(entity);
        }
    }

    @Override
    public boolean existsById(UUID id) {
        Long count = entityManager.createQuery(
                        "select count(r) from JpaReservationEntity r where r.reservationId = :id", Long.class)
                .setParameter("id", id)
                .getSingleResult();
        return count > 0;
    }

    @Override
    public Reservation findById(UUID id) {
        JpaReservationEntity entity = entityManager.find(JpaReservationEntity.class, id);
        return entity != null ? entity.toDomain(new MapperContext()) : null;
    }

    @Override
    public boolean existsByRiderId(UUID riderId) {
        Long count = entityManager.createQuery(
                        "select count(r) from JpaReservationEntity r where r.rider.userId = :riderId and r.active = true", Long.class)
                .setParameter("riderId", riderId)
                .getSingleResult();
        return count > 0;
    }

    @Override
    public Reservation findByRiderId(UUID riderId) {
        return entityManager.createQuery(
                        "select r from JpaReservationEntity r where r.rider.userId = :riderId and r.active = true", JpaReservationEntity.class)
                .setParameter("riderId", riderId)
                .getResultStream()
                .findFirst()
                .map(entity -> entity.toDomain(new MapperContext()))
                .orElse(null);
    }

    @Override
    public List<Reservation> findExpiredReservations() {
        Instant now = Instant.now();
        MapperContext context = new MapperContext();
        return entityManager.createQuery(
                        "select r from JpaReservationEntity r where r.expiresAt <= :now and r.active = true",
                        JpaReservationEntity.class)
                .setParameter("now", now)
                .getResultStream()
                .map(entity -> entity.toDomain(context))
                .collect(Collectors.toList());
    }
}
