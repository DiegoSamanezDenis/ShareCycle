package com.sharecycle.domain.repository;

import com.sharecycle.domain.model.Bike;

import java.util.List;
import java.util.UUID;

public interface JpaBikeRepository {
    Bike findById(UUID id);
    void save(Bike bike);
    List<Bike> findAll();
    List<Bike> findByCurrentStationId(UUID stationId);
}
