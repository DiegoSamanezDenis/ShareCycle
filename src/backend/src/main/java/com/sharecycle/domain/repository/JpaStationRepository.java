package com.sharecycle.domain.repository;

import com.sharecycle.model.entity.Station;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface JpaStationRepository extends JpaRepository<Station, UUID> {

}
