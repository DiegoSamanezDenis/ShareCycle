package com.sharecycle.infrastructure;

import com.sharecycle.domain.repository.JpaLedgerEntryRepository;
import com.sharecycle.model.entity.LedgerEntry;
import com.sharecycle.model.entity.Trip;
import com.sharecycle.model.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class JpaLedgerEntryRepositoryImpl implements JpaLedgerEntryRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public void save(LedgerEntry ledgerEntry) {
        em.merge(ledgerEntry);
    }

    @Override
    public LedgerEntry findById(UUID id) {
        return em.find(LedgerEntry.class, id);
    }

    @Override
    public LedgerEntry findByTrip(Trip trip) {
        return em.createQuery(
                        "SELECT l FROM LedgerEntry l WHERE l.trip_id = :trip_id", LedgerEntry.class)
                .setParameter("trip_id", trip.getTripID())
                .getSingleResult();
    }

    @Override
    public List<LedgerEntry> findAllByUser(User user) {
        return em.createQuery(
                        "SELECT l FROM LedgerEntry l WHERE l.user_id = :user_id", LedgerEntry.class)
                .setParameter("user_id", user.getUserId())
                .getResultList();
    }
}
