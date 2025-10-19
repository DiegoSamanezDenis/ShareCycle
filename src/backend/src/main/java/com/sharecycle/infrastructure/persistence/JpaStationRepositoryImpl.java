package com.sharecycle.infrastructure.persistence;

import com.sharecycle.domain.model.Station;
import com.sharecycle.domain.repository.JpaStationRepository;
import com.sharecycle.infrastructure.persistence.jpa.JpaStationEntity;
import com.sharecycle.infrastructure.persistence.jpa.MapperContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@Transactional
public class JpaStationRepositoryImpl implements JpaStationRepository {

    @PersistenceContext
    private final EntityManager entityManager;

    public JpaStationRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Station findById(UUID id) {
        JpaStationEntity entity = entityManager.find(JpaStationEntity.class, id);
        return entity != null ? entity.toDomain(new MapperContext()) : null;
    }

    @Override
    public Station findByIdForUpdate(UUID id) {
        JpaStationEntity entity = entityManager.find(JpaStationEntity.class, id, LockModeType.PESSIMISTIC_WRITE);
        return entity != null ? entity.toDomain(new MapperContext()) : null;
    }

    @Override
    public List<Station> findAll() {
        MapperContext context = new MapperContext();
        return entityManager.createQuery("from JpaStationEntity", JpaStationEntity.class)
                .getResultStream()
                .map(entity -> entity.toDomain(context))
                .collect(Collectors.toList());
    }

    @Override
    public void save(Station station) {
        MapperContext context = new MapperContext();
        JpaStationEntity entity = JpaStationEntity.fromDomain(station, context);
        if (entity.getStationId() == null) {
            entityManager.persist(entity);
        } else {
            entityManager.merge(entity);
        }
    }
}
