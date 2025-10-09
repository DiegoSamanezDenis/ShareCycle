package com.sharecycle.infrastructure;

import com.sharecycle.domain.repository.JpaStationRepository;
import com.sharecycle.model.entity.Station;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.UUID;

public class JpaStationRepositoryImpl implements JpaStationRepository {
    @PersistenceContext
    private EntityManager em;

    @Override
    public Station findById(UUID id) {
        return em.find(Station.class, id);
    }
}
