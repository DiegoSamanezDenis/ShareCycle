package com.sharecycle.infrastructure.persistence;

import com.sharecycle.domain.model.Dock;
import com.sharecycle.domain.repository.JpaDockRepository;
import com.sharecycle.infrastructure.persistence.jpa.JpaDockEntity;
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
public class JpaDockRepositoryImpl implements JpaDockRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void save(Dock dock) {
        MapperContext context = new MapperContext();
        JpaDockEntity entity = JpaDockEntity.fromDomain(dock, context);
        if (entity.getDockId() == null) {
            entityManager.persist(entity);
        } else {
            entityManager.merge(entity);
        }
    }

    @Override
    public Dock findById(UUID id) {
        JpaDockEntity entity = entityManager.find(JpaDockEntity.class, id);
        return entity != null ? entity.toDomain(new MapperContext()) : null;
    }

    @Override
    public List<Dock> findAll() {
        MapperContext context = new MapperContext();
        return entityManager.createQuery("from JpaDockEntity", JpaDockEntity.class)
                .getResultStream()
                .map(entity -> entity.toDomain(context))
                .collect(Collectors.toList());
    }

    @Override
    public int clearBikeFromAllDocks(UUID bikeId) {
        if (bikeId == null) {
            return 0;
        }
        // Clear bike from any dock that currently references it, and mark dock as EMPTY
        return entityManager.createQuery(
                        "update JpaDockEntity d set d.occupiedBike = null, d.status = com.sharecycle.domain.model.Dock$DockStatus.EMPTY where d.occupiedBike.bikeId = :bikeId")
                .setParameter("bikeId", bikeId)
                .executeUpdate();
    }
}
