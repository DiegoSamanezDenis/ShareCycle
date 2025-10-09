package com.sharecycle.domain.repository;

import com.sharecycle.model.entity.Dock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaDockRepository extends JpaRepository<Dock, UUID> {
}
