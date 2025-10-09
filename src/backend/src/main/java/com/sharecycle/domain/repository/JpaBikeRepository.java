package com.sharecycle.domain.repository;


import com.sharecycle.model.entity.Bike;

import java.util.UUID;

public interface JpaBikeRepository{
    Bike findById(UUID id);
    void save(Bike bike);
}