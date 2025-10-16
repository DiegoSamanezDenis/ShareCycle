package com.sharecycle.service;

import org.springframework.stereotype.Service;

import java.util.UUID;

public interface SessionStore {
    String createSession(UUID userId);
    UUID getUserId(String token);
    void invalidate(String token);
    
}
