package com.sharecycle.service;

import java.util.*;
import java.util.UUID;

public class InMemorySessionStore implements SessionStore {
    private final Map<String, UUID> sessions = new HashMap<>();

    @Override 
    public String createSession(UUID userId) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, userId);
        return token;
    }

    @Override 
    public UUID getUserId(String token) {
        return sessions.get(token);
    }

    @Override 
    public void invalidate(String token){
        sessions.remove(token);
    }
}