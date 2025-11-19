package com.sharecycle.application;

import com.sharecycle.application.exception.InvalidCredentialsException;
import com.sharecycle.domain.model.User;
import com.sharecycle.domain.repository.UserRepository;
import com.sharecycle.service.PasswordHasher;
import com.sharecycle.service.SessionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class LoginUseCase {
    private static final Logger logger = LoggerFactory.getLogger(LoginUseCase.class);

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final SessionStore sessionStore;
    private final CheckTierStatusUseCase checkTierStatusUseCase;

    public LoginUseCase(UserRepository userRepository, PasswordHasher passwordHasher, SessionStore sessionStore, CheckTierStatusUseCase checkTierStatusUseCase) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.sessionStore = sessionStore;
        this.checkTierStatusUseCase = checkTierStatusUseCase;
    }

    public LoginResponse execute(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new InvalidCredentialsException("Username and password are required");
        }
        String normalizedUsername = username.trim();

        Optional<User> userOpt = userRepository.findByUsername(normalizedUsername);
        if (userOpt.isEmpty()) {
            throw new InvalidCredentialsException("Invalid username or password");
        }
        User user = userOpt.get();
        if (!passwordHasher.verify(password, user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        if ("RIDER".equalsIgnoreCase(user.getRole())) {
            try {
                checkTierStatusUseCase.execute(user.getUserId());
            } catch (Exception e) {
                // We catch exceptions here so that a failure in the loyalty system
                // does not prevent the user from logging in.
                logger.warn("Failed to evaluate loyalty tier for user {}", user.getUserId(), e);
            }
        }

        String token = sessionStore.createSession(user.getUserId());
        return new LoginResponse(user.getUserId(), user.getUsername(), user.getRole(), token);
    }


    public record LoginResponse(UUID userId, String username, String role, String token) {
    }
}
