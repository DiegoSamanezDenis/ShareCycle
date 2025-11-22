package com.sharecycle.application;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sharecycle.domain.model.User;
import com.sharecycle.domain.repository.UserRepository;
import com.sharecycle.service.SessionStore;

@Service
public class ToggleRoleUseCase {
    private static final Logger logger = LoggerFactory.getLogger(ToggleRoleUseCase.class);

    private final UserRepository userRepository;
    private final SessionStore sessionStore;

    public ToggleRoleUseCase(UserRepository userRepository, SessionStore sessionStore) {
        this.userRepository = userRepository;
        this.sessionStore = sessionStore;
    }

    /**
     * Toggle the role mode for an operator between OPERATOR and RIDER
     * 
     * @param token The session token
     * @return The new mode after toggling
     * @throws IllegalStateException if user is not an operator or not found
     */
    public ToggleRoleResponse execute(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token is required");
        }

        UUID userId = sessionStore.getUserId(token);
        if (userId == null) {
            throw new IllegalStateException("Invalid or expired session");
        }

        User user = userRepository.findById(userId);
        if (user == null) {
            throw new IllegalStateException("User not found");
        }

        if (!"OPERATOR".equals(user.getRole())) {
            throw new IllegalStateException("Only operators can toggle roles");
        }

        // Get current mode from session
        String currentMode = sessionStore.getEffectiveRole(token, user.getRole());
        if (currentMode == null) {
            currentMode = "OPERATOR"; // Default to OPERATOR if not set
        }

        // Toggle the mode
        String newMode = "OPERATOR".equals(currentMode) ? "RIDER" : "OPERATOR";

        // Update session state
        sessionStore.setOperatorMode(token, newMode);

        logger.info("Operator {} toggled mode to {}", userId, newMode);

        return new ToggleRoleResponse(
                userId,
                user.getUsername(),
                user.getRole(),
                newMode,
                token
        );
    }

    /**
     * Response containing the updated role information
     * 
     * @param userId The user's ID
     * @param username The username
     * @param baseRole The base role (always "OPERATOR")
     * @param currentMode The current mode ("OPERATOR" or "RIDER")
     * @param token The session token
     */
    public record ToggleRoleResponse(
            UUID userId,
            String username,
            String baseRole,
            String currentMode,
            String token
    ) {}
}
