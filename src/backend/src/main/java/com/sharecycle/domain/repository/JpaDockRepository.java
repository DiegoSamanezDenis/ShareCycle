package com.sharecycle.domain.repository;

import com.sharecycle.domain.model.Dock;

import java.util.List;
import java.util.UUID;

public interface JpaDockRepository{
    void save(Dock dock);
    Dock findById(UUID id);
    List<Dock> findAll();
    /**
     * Clears any existing dock assignment for the given bike across all stations.
     * Returns the number of affected rows.
     */
    int clearBikeFromAllDocks(UUID bikeId);
}
