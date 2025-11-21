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
    private EntityManager entityManager;

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
        Instant now = Instant.now();
        Long count = entityManager.createQuery(
                        "select count(r) from JpaReservationEntity r where r.rider.userId = :riderId and r.active = true and r.expiresAt > :now",
                        Long.class)
                .setParameter("riderId", riderId)
                .setParameter("now", now)
                .getSingleResult();
        return count > 0;
    }

    @Override
    public Reservation findByRiderId(UUID riderId) {
        Instant now = Instant.now();
        return entityManager.createQuery(
                        "select r from JpaReservationEntity r where r.rider.userId = :riderId and r.active = true and r.expiresAt > :now",
                        JpaReservationEntity.class)
                .setParameter("riderId", riderId)
                .setParameter("now", now)
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

    @Override
    public boolean hasActiveReservationForBike(UUID bikeId) {
        Instant now = Instant.now();
        Long count = entityManager.createQuery(
                        "select count(r) from JpaReservationEntity r where r.bike.bikeId = :bikeId and r.active = true and r.expiresAt > :now",
                        Long.class)
                .setParameter("bikeId", bikeId)
                .setParameter("now", now)
                .getSingleResult();
        return count > 0;
    }

    @Override
    public int countReservationsByRiderIdAfter(UUID riderId, Instant since) {
        Long count = entityManager.createQuery(
                        "select count(r) from JpaReservationEntity r where r.rider.userId = :riderId and r.reservedAt > :since",
                        Long.class).setParameter("riderId", riderId).setParameter("since", since).getSingleResult();

        return count.intValue();
    }
}
