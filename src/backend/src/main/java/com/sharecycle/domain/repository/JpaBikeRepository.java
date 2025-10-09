package com.sharecycle.domain.repository;


import com.sharecycle.model.entity.Bike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaBikeRepository extends JpaRepository<Bike, UUID> {

}