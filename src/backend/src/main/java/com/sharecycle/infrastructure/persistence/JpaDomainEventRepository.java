package com.sharecycle.infrastructure.persistence;

import com.sharecycle.infrastructure.persistence.jpa.JpaDomainEventEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Transactional
public class JpaDomainEventRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public void save(JpaDomainEventEntity entity) {
        entityManager.persist(entity);
    }

    public List<JpaDomainEventEntity> findRecent(int limit) {
        return entityManager.createQuery(
                        "select e from JpaDomainEventEntity e order by e.occurredAt desc",
                        JpaDomainEventEntity.class)
                .setMaxResults(limit)
                .getResultList();
    }
}
