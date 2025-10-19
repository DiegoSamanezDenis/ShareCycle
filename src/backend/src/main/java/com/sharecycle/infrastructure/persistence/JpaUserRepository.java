package com.sharecycle.infrastructure.persistence;

import com.sharecycle.domain.model.User;
import com.sharecycle.domain.repository.UserRepository;
import com.sharecycle.infrastructure.persistence.jpa.JpaUserEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional
public class JpaUserRepository implements UserRepository {

    @PersistenceContext
    private final EntityManager entityManager;

    public JpaUserRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public boolean existsByEmail(String email) {
        Long count = entityManager.createQuery(
                        "select count(u) from JpaUserEntity u where u.email = :email", Long.class)
                .setParameter("email", email)
                .getSingleResult();
        return count > 0;
    }

    @Override
    public boolean existsByUsername(String username) {
        Long count = entityManager.createQuery(
                        "select count(u) from JpaUserEntity u where u.username = :username", Long.class)
                .setParameter("username", username)
                .getSingleResult();
        return count > 0;
    }

    @Override
    public void save(User user) {
        JpaUserEntity entity = JpaUserEntity.fromDomain(user);
        if (entity.getUserId() == null) {
            entityManager.persist(entity);
            entityManager.flush();
            user.setUserId(entity.getUserId());
        } else {
            JpaUserEntity merged = entityManager.merge(entity);
            user.setUserId(merged.getUserId());
        }
    }

    @Override
    public User findById(UUID id) {
        JpaUserEntity entity = entityManager.find(JpaUserEntity.class, id);
        return entity != null ? entity.toDomain() : null;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return entityManager.createQuery(
                        "select u from JpaUserEntity u where u.username = :username", JpaUserEntity.class)
                .setParameter("username", username)
                .getResultStream()
                .map(JpaUserEntity::toDomain)
                .findFirst();
    }
}
