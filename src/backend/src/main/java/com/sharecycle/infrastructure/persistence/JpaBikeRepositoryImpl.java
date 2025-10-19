package com.sharecycle.infrastructure.persistence;

import com.sharecycle.domain.model.Bike;
import com.sharecycle.domain.repository.JpaBikeRepository;
import com.sharecycle.infrastructure.persistence.jpa.JpaBikeEntity;
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
public class JpaBikeRepositoryImpl implements JpaBikeRepository {

    @PersistenceContext
    private final EntityManager entityManager;

    public JpaBikeRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Bike findById(UUID id) {
        JpaBikeEntity entity = entityManager.find(JpaBikeEntity.class, id);
        return entity != null ? entity.toDomain(new MapperContext()) : null;
    }

    @Override
    public void save(Bike bike) {
        MapperContext context = new MapperContext();
        JpaBikeEntity entity = JpaBikeEntity.fromDomain(bike, context);
        if (entity.getBikeId() == null) {
            entityManager.persist(entity);
        } else {
            entityManager.merge(entity);
        }
    }

    @Override
    public List<Bike> findAll() {
        MapperContext context = new MapperContext();
        return entityManager.createQuery("from JpaBikeEntity", JpaBikeEntity.class)
                .getResultStream()
                .map(entity -> entity.toDomain(context))
                .collect(Collectors.toList());
    }

    @Override
    public List<Bike> findByCurrentStationId(UUID stationId) {
        MapperContext context = new MapperContext();
        return entityManager.createQuery(
                        "select b from JpaBikeEntity b where b.currentStation.stationId = :stationId", JpaBikeEntity.class)
                .setParameter("stationId", stationId)
                .getResultStream()
                .map(entity -> entity.toDomain(context))
                .collect(Collectors.toList());
    }
}
