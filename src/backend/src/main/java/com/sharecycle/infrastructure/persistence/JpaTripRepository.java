package com.sharecycle.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sharecycle.domain.model.Bike;
import com.sharecycle.domain.model.Trip;
import com.sharecycle.domain.repository.TripRepository;
import com.sharecycle.infrastructure.persistence.jpa.JpaTripEntity;
import com.sharecycle.infrastructure.persistence.jpa.MapperContext;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;

@Repository
@Transactional
public class JpaTripRepository implements TripRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void save(Trip trip) {
        MapperContext context = new MapperContext(entityManager);
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
    public Trip findMostRecentCompletedByUserId(UUID userId) {
        return entityManager.createQuery(
                        "select t from JpaTripEntity t where t.rider.userId = :userId and t.endTime is not null order by t.endTime desc",
                        JpaTripEntity.class)
                .setParameter("userId", userId)
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .map(entity -> entity.toDomain(new MapperContext()))
                .orElse(null);
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
        return fetchTrips(null, startDate, endDate, bikeType, null, null);
    }

    @Override
    public List<Trip> findAllWithFilterPaged(LocalDateTime startDate, LocalDateTime endDate, Bike.BikeType bikeType, int page, int pageSize) {
        return fetchTrips(null, startDate, endDate, bikeType, page, pageSize);
    }

    @Override
    public long countAllWithFilter(LocalDateTime startDate, LocalDateTime endDate, Bike.BikeType bikeType) {
        return countTrips(null, startDate, endDate, bikeType);
    }

    @Override
    public List<Trip> findAllByUserIdWithFilter(UUID userId, LocalDateTime startDate, LocalDateTime endDate, Bike.BikeType bikeType) {
        return fetchTrips(userId, startDate, endDate, bikeType, null, null);
    }

    @Override
    public List<Trip> findAllByUserIdWithFilterPaged(UUID userId, LocalDateTime startDate, LocalDateTime endDate, Bike.BikeType bikeType, int page, int pageSize) {
        return fetchTrips(userId, startDate, endDate, bikeType, page, pageSize);
    }

    @Override
    public long countAllByUserIdWithFilter(UUID userId, LocalDateTime startDate, LocalDateTime endDate, Bike.BikeType bikeType) {
        return countTrips(userId, startDate, endDate, bikeType);
    }

    // clearAssociationsForTrip removed to avoid nulling rider/bike which breaks toDomain

    // archiveTrip removed; not part of repository interface

    private List<Trip> fetchTrips(UUID userId,
                                  LocalDateTime startDate,
                                  LocalDateTime endDate,
                                  Bike.BikeType bikeType,
                                  Integer page,
                                  Integer pageSize) {
        String queryStr = buildTripQuery(userId, startDate, endDate, bikeType, false);
        TypedQuery<JpaTripEntity> query = entityManager.createQuery(queryStr, JpaTripEntity.class);
        applyFilterParameters(query, userId, startDate, endDate, bikeType);
        if (page != null && pageSize != null) {
            int safePage = Math.max(0, page);
            int safePageSize = Math.max(1, pageSize);
            query.setFirstResult(safePage * safePageSize);
            query.setMaxResults(safePageSize);
        }
        MapperContext context = new MapperContext();
        return query.getResultList().stream()
                .map(entity -> entity.toDomain(context))
                .toList();
    }

    private long countTrips(UUID userId,
                            LocalDateTime startDate,
                            LocalDateTime endDate,
                            Bike.BikeType bikeType) {
        String queryStr = buildTripQuery(userId, startDate, endDate, bikeType, true);
        TypedQuery<Long> query = entityManager.createQuery(queryStr, Long.class);
        applyFilterParameters(query, userId, startDate, endDate, bikeType);
        return query.getSingleResult();
    }

    private String buildTripQuery(UUID userId,
                                  LocalDateTime startDate,
                                  LocalDateTime endDate,
                                  Bike.BikeType bikeType,
                                  boolean countQuery) {
        StringBuilder queryStr = new StringBuilder(countQuery ? "SELECT COUNT(t) FROM JpaTripEntity t" : "SELECT t FROM JpaTripEntity t");
        List<String> predicates = new ArrayList<>();
        if (userId != null) {
            predicates.add("t.rider.userId = :userId");
        }
        if (bikeType != null) {
            predicates.add("t.bike.type = :bikeType");
        }
        if (startDate != null) {
            predicates.add("t.startTime >= :startDate");
        }
        if (endDate != null) {
            predicates.add("t.endTime <= :endDate");
        }
        if (!predicates.isEmpty()) {
            queryStr.append(" WHERE ").append(String.join(" AND ", predicates));
        }
        if (!countQuery) {
            queryStr.append(" ORDER BY CASE WHEN t.endTime IS NULL THEN 1 ELSE 0 END, t.endTime DESC, t.startTime DESC");
        }
        return queryStr.toString();
    }

    private void applyFilterParameters(TypedQuery<?> query,
                                       UUID userId,
                                       LocalDateTime startDate,
                                       LocalDateTime endDate,
                                       Bike.BikeType bikeType) {
        if (userId != null) {
            query.setParameter("userId", userId);
        }
        if (bikeType != null) {
            query.setParameter("bikeType", bikeType);
        }
        if (startDate != null) {
            query.setParameter("startDate", startDate);
        }
        if (endDate != null) {
            query.setParameter("endDate", endDate);
        }
    }
}
