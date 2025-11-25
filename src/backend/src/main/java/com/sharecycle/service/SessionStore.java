package com.sharecycle.service;

import java.util.UUID;

public interface SessionStore {
    String createSession(UUID userId);
    UUID getUserId(String token);
    void invalidate(String token);
    
    /**
     * Set the current mode for an operator session
     * @param token The session token
     * @param mode The mode to set ("OPERATOR" or "RIDER")
     */
    void setOperatorMode(String token, String mode);
    
    /**
     * Get the current mode for an operator session
     * @param token The session token
     * @return The current mode, or null if not set or not an operator session
     */
    String getOperatorMode(String token);
    
    /**
     * Get the effective role for a session, considering operator mode if applicable
     * @param token The session token
     * @param baseRole The base role of the user ("OPERATOR", "RIDER", etc.)
     * @return The effective role to use for authorization
     */
    String getEffectiveRole(String token, String baseRole);
}
