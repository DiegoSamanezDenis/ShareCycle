package com.sharecycle.domain.repository;

import com.sharecycle.model.entity.Station;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface JpaStationRepository{
    Station findById(UUID id);
}
