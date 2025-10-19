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
    private final EntityManager entityManager;

    public JpaDockRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

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
}
