package com.sharecycle.domain.repository;

import com.sharecycle.domain.model.Dock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaDockRepository{
    void save(Dock dock);
    Dock findById(UUID id);
    List<Dock> findAll();
}
