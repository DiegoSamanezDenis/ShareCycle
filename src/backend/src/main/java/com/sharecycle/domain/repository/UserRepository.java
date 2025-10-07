package com.sharecycle.domain.repository;

import com.sharecycle.model.entity.User;
public interface UserRepository {
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    void save(User user);


}
