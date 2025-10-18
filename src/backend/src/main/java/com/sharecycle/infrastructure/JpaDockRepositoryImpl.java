package com.sharecycle.infrastructure;

import com.sharecycle.domain.repository.JpaDockRepository;
import com.sharecycle.model.entity.Dock;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class JpaDockRepositoryImpl implements JpaDockRepository {

    @PersistenceContext
    private EntityManager em;


    @Override
    public void save(Dock dock) {
        em.merge(dock);
    }

    @Override
    public Dock findById(UUID id) {
        return em.find(Dock.class, id);
    }

    @Override
    public List<Dock> findAll() {
        return em.createQuery("select d from Dock d", Dock.class).getResultList();
    }
}
