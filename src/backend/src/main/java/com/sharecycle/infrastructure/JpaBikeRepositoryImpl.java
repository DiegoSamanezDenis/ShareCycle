package com.sharecycle.infrastructure;

import com.sharecycle.domain.repository.JpaBikeRepository;
import com.sharecycle.model.entity.Bike;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class JpaBikeRepositoryImpl implements JpaBikeRepository {
    @PersistenceContext
    private EntityManager em;

    @Override
    public Bike findById(UUID id) {
        return em.find(Bike.class, id);
    }

    @Override
    public void save(Bike bike) {
        em.merge(bike);
    }

    @Override
    public List<Bike> findAll() {
        return em.createQuery("from Bike", Bike.class).getResultList();
    }
}
