package com.sharecycle.domain.repository;

import com.sharecycle.domain.model.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    void save(User user);
    User findById(UUID id);
    Optional<User> findByUsername(String username);
}
