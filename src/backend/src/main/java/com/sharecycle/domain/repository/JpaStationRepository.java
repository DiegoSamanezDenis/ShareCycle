package com.sharecycle.domain.repository;

import com.sharecycle.domain.model.Station;

import java.util.List;
import java.util.UUID;

public interface JpaStationRepository {
    Station findById(UUID id);
    Station findByIdForUpdate(UUID id);
    List<Station> findAll();
    void save(Station station);
}
