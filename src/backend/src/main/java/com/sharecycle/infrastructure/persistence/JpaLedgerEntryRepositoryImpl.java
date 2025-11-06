package com.sharecycle.infrastructure.persistence;

import com.sharecycle.domain.model.LedgerEntry;
import com.sharecycle.domain.model.Trip;
import com.sharecycle.domain.model.User;
import com.sharecycle.domain.repository.JpaLedgerEntryRepository;
import com.sharecycle.infrastructure.persistence.jpa.JpaLedgerEntryEntity;
import com.sharecycle.infrastructure.persistence.jpa.MapperContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@Transactional
public class JpaLedgerEntryRepositoryImpl implements JpaLedgerEntryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void save(LedgerEntry ledgerEntry) {
        MapperContext context = new MapperContext();
        JpaLedgerEntryEntity entity = JpaLedgerEntryEntity.fromDomain(ledgerEntry, context);
        if (entity.getLedgerId() == null) {
            entityManager.persist(entity);
        } else {
            entityManager.merge(entity);
        }
    }

    @Override
    public LedgerEntry findById(UUID id) {
        JpaLedgerEntryEntity entity = entityManager.find(JpaLedgerEntryEntity.class, id);
        return entity != null ? entity.toDomain(new MapperContext()) : null;
    }

    @Override
    public LedgerEntry findByTrip(Trip trip) {
        MapperContext context = new MapperContext();
        return entityManager.createQuery(
                        "select l from JpaLedgerEntryEntity l where l.trip.tripId = :tripId", JpaLedgerEntryEntity.class)
                .setParameter("tripId", trip.getTripID())
                .getSingleResult()
                .toDomain(context);
    }

    @Override
    public List<LedgerEntry> findAllByUser(User user) {
        MapperContext context = new MapperContext();
        return entityManager.createQuery(
                        "select l from JpaLedgerEntryEntity l where l.user.userId = :userId", JpaLedgerEntryEntity.class)
                .setParameter("userId", user.getUserId())
                .getResultStream()
                .map(entity -> entity.toDomain(context))
                .collect(Collectors.toList());
    }
}
