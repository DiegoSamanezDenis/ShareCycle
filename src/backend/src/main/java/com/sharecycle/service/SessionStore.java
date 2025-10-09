package com.sharecycle.service;

import java.util.UUID;

public interface SessionStore {
    String createSession(UUID userId);
    UUID getUserId(String token);
    void invalidate(String token);
    
}
