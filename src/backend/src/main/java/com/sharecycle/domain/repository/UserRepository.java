package com.sharecycle.domain.repository;

import com.sharecycle.model.entity.User;
import java.util.Optional;
public interface UserRepository {
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    void save(User user);
    Optional<User> findByUsername(String username);


}
