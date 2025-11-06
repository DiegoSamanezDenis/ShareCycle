package com.sharecycle.infrastructure.persistence;

import com.sharecycle.domain.model.Bike;
import com.sharecycle.domain.model.Trip;
import com.sharecycle.domain.repository.TripRepository;
import com.sharecycle.infrastructure.persistence.jpa.JpaTripEntity;
import com.sharecycle.infrastructure.persistence.jpa.MapperContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
@Transactional
public class JpaTripRepository implements TripRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void save(Trip trip) {
        MapperContext context = new MapperContext();
        JpaTripEntity entity = JpaTripEntity.fromDomain(trip, context);
        // Upsert by trip_id
        entityManager.merge(entity);
    }

    @Override
    public Trip findById(UUID id) {
        JpaTripEntity entity = entityManager.find(JpaTripEntity.class, id);
        return entity != null ? entity.toDomain(new MapperContext()) : null;
    }

    @Override
    public void deleteById(UUID id) {
        JpaTripEntity entity = entityManager.find(JpaTripEntity.class, id);
        if (entity != null) {
            entityManager.remove(entity);
        }
    }

    @Override
    public boolean riderHasActiveTrip(UUID riderId) {
        Long count = entityManager.createQuery(
                        "select count(t) from JpaTripEntity t where t.rider.userId = :riderId and t.endTime is null",
                        Long.class)
                .setParameter("riderId", riderId)
                .getSingleResult();
        return count > 0;
    }

    @Override
    public Trip findByUserId(UUID userId) {
        return entityManager.createQuery(
                        "select t from JpaTripEntity t where t.rider.userId = :userId and t.endTime is null",
                        JpaTripEntity.class)
                .setParameter("userId", userId)
                .getResultStream()
                .findFirst()
                .map(entity -> entity.toDomain(new MapperContext()))
                .orElse(null);
    }

    @Override
    public Trip findByBikeId(UUID bikeId) {
        return entityManager.createQuery(
                        "select t from JpaTripEntity t where t.bike.bikeId = :bikeId and t.endTime is null",
                        JpaTripEntity.class)
                .setParameter("bikeId", bikeId)
                .getResultStream()
                .findFirst()
                .map(entity -> entity.toDomain(new MapperContext()))
                .orElse(null);
    }

    @Override
    public void deleteByUserId(UUID userId) {
        entityManager.createQuery("delete from JpaTripEntity t where t.rider.userId = :userId")
                .setParameter("userId", userId)
                .executeUpdate();
    }

    @Override
    public void deleteByBikeId(UUID bikeId) {
        entityManager.createQuery("delete from JpaTripEntity t where t.bike.bikeId = :bikeId")
                .setParameter("bikeId", bikeId)
                .executeUpdate();
    }
    @Override
    public List<Trip> findAll(){
        return entityManager.createQuery("select t from JpaTripEntity t ", JpaTripEntity.class).getResultList().stream()
                .map(entity -> entity.toDomain(new MapperContext())) // define a mapping method
                .toList();
    }
    @Override
    public List<Trip> findAllByUserId(UUID userId) {
        return entityManager.createQuery(
                        "select t from JpaTripEntity t where t.rider.userId = :userId",
                        JpaTripEntity.class)
                .setParameter("userId", userId)
                .getResultStream()
                .map(entity -> entity.toDomain(new MapperContext()))
                .toList();
    }

    @Override
    public List<Trip> findAllWithFilter(LocalDateTime startDate, LocalDateTime endDate, Bike.BikeType bikeType) {
        StringBuilder queryStr = new StringBuilder(
                "SELECT t FROM JpaTripEntity t"
        );


        if (bikeType != null || startDate != null || endDate != null) queryStr.append(" WHERE ");
        if (bikeType != null) queryStr.append(" AND t.bike.type = :bikeType");
        if (startDate != null) queryStr.append(" AND t.startTime >= :startDate");
        if (endDate != null) queryStr.append(" AND t.endTime <= :endDate");
        var query = entityManager.createQuery(queryStr.toString(), JpaTripEntity.class);

        if (bikeType != null) query.setParameter("bikeType", bikeType);
        if (startDate != null) query.setParameter("startDate", startDate);
        if (endDate != null) query.setParameter("endDate", endDate);

        return query.getResultList().stream()
                .map(entity -> entity.toDomain(new MapperContext()))
                .toList();
    }
    @Override
    public List<Trip> findAllByUserIdWithFilter(UUID userId, LocalDateTime startDate, LocalDateTime endDate, Bike.BikeType bikeType) {
        StringBuilder queryStr = new StringBuilder(
                "SELECT t FROM JpaTripEntity t WHERE t.rider.userId = :userId"
        );

        if (bikeType != null) queryStr.append(" AND t.bike.type = :bikeType");
        if (startDate != null) queryStr.append(" AND t.startTime >= :startDate");
        if (endDate != null) queryStr.append(" AND t.endTime <= :endDate");

        var query = entityManager.createQuery(queryStr.toString(), JpaTripEntity.class)
                .setParameter("userId", userId);

        if (bikeType != null) query.setParameter("bikeType", bikeType);
        if (startDate != null) query.setParameter("startDate", startDate);
        if (endDate != null) query.setParameter("endDate", endDate);

        return query.getResultList().stream()
                .map(entity -> entity.toDomain(new MapperContext()))
                .toList();
    }

    // clearAssociationsForTrip removed to avoid nulling rider/bike which breaks toDomain

    // archiveTrip removed; not part of repository interface
}
