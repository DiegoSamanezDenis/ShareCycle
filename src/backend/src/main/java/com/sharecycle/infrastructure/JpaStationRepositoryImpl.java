package com.sharecycle.infrastructure;

import com.sharecycle.domain.repository.JpaStationRepository;
import com.sharecycle.model.entity.Station;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class JpaStationRepositoryImpl implements JpaStationRepository {
    @PersistenceContext
    private EntityManager em;

    @Override
    public Station findById(UUID id) {
        return em.find(Station.class, id);
    }

    @Override
    public List<Station> findAll(){
        return em.createQuery("from Station", Station.class).getResultList();
    }

    @Override
    public void save(Station station) {
        em.merge(station);
    }
}
