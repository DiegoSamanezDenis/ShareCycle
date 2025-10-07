package com.sharecycle.infrastructure;

import com.sharecycle.domain.repository.UserRepository;
import com.sharecycle.model.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

// this implements the UserREpository interface
public class JpaUserRepository implements UserRepository {

    private final EntityManager em;

    public JpaUserRepository(EntityManager em) {
        this.em = em;
    }


    @Override
    public boolean existsByEmail(String email) {
        TypedQuery<Long> query = em.createQuery(
                "select count(u) from User u where u.email = :email", Long.class);

        query.setParameter("email", email);
        return query.getSingleResult() > 0;
    }

    @Override
    public boolean existsByUsername(String username) {
        TypedQuery<Long> query = em.createQuery(
                "select count(u) from User u where u.username = :username", Long.class
        );
        query.setParameter("username", username);
        return query.getSingleResult() > 0;
    }

    @Override
    public void save(User user) {

        em.persist(user);
    }
}
