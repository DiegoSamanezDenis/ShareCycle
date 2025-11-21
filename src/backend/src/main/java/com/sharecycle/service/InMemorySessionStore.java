package com.sharecycle.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class InMemorySessionStore implements SessionStore {
    private final Map<String, UUID> sessions = new HashMap<>();
    private final Map<String, String> operatorModes = new HashMap<>();

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
        operatorModes.remove(token);
    }
    
    @Override
    public void setOperatorMode(String token, String mode) {
        if (sessions.containsKey(token)) {
            operatorModes.put(token, mode);
        }
    }
    
    @Override
    public String getOperatorMode(String token) {
        return operatorModes.get(token);
    }
    
    @Override
    public String getEffectiveRole(String token, String baseRole) {
        // If user is an operator and has a mode set, use that mode
        if ("OPERATOR".equals(baseRole)) {
            String mode = operatorModes.get(token);
            if (mode != null) {
                return mode;
            }
            // Default to OPERATOR if no mode is set
            return "OPERATOR";
        }
        // For non-operators, return their base role
        return baseRole;
    }
}
