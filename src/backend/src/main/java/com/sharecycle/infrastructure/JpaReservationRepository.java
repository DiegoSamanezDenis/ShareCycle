package com.sharecycle.infrastructure;

import com.sharecycle.domain.repository.ReservationRepository;
import com.sharecycle.model.entity.Reservation;
import org.springframework.stereotype.Repository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class JpaReservationRepository implements ReservationRepository {

    private EntityManager entityManager;

    @Override
    public void save(Reservation reservation) {
        if (reservation.getReservationId() == null) {
            entityManager.persist(reservation);
        }

        else {
            entityManager.merge(reservation);
        }
    }

    @Override
    public Reservation findByRiderId(UUID riderId) {
        String jpql = "SELECT r FROM Reservation r WHERE r.rider.userId = :riderId AND r.active = true";
        TypedQuery<Reservation> query = entityManager.createQuery(jpql, Reservation.class)
                .setParameter("riderId", riderId);

        List<Reservation> results = query.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public List<Reservation> findExpiredReservations() {
        String jpql = "SELECT r FROM Reservation r WHERE r.expiresAt <= :now AND r.active = true";
        return entityManager.createQuery(jpql, Reservation.class)
                .setParameter("now", LocalDateTime.now())
                .getResultList();
    }

    @Override
    public boolean existsByRiderId(UUID riderId) {
        String jpql = "SELECT COUNT(r) FROM Reservation r WHERE r.rider.userId = :riderId AND r.active = true";
        Long count = entityManager.createQuery(jpql, Long.class)
                .setParameter("riderId", riderId)
                .getSingleResult();
        return count > 0;
    }

    @Override
    public Reservation findById(UUID id) {
        return entityManager.find(Reservation.class, id);
    }

    @Override
    public boolean existsById(UUID id) {
        String jpql = "SELECT COUNT(r) FROM Reservation r WHERE r.reservationId = :id";
        Long count = entityManager.createQuery(jpql, Long.class)
                .setParameter("id", id)
                .getSingleResult();
        return count > 0;
    }
}