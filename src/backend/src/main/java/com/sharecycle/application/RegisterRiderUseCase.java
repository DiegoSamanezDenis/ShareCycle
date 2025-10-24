package com.sharecycle.application;

import com.sharecycle.domain.repository.UserRepository;
import com.sharecycle.domain.model.Rider;
import com.sharecycle.service.PasswordHasher;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class RegisterRiderUseCase {
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    //constructor
    public RegisterRiderUseCase(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    public RegistrationResult register(String fullName, String address, String email, String username, String password, String paymentToken) {
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Full name is required");
        }
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("Address is required");
        }
        if (email == null || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("Email address is invalid");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }
        if (paymentToken == null || paymentToken.isBlank()) {
            throw new IllegalArgumentException("Payment method token is required");
        }
        String normalizedUsername = username.trim();
        String normalizedEmail = email.trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail) || userRepository.existsByUsername(normalizedUsername.toLowerCase())) {
            throw new IllegalArgumentException("Username/email is already in use");
        }
        if (password.length() > 72) {
            throw new IllegalArgumentException("Password is too long");
        }
        String hashedPassword = passwordHasher.hash(password);
        Rider rider = new Rider(fullName.trim(), address.trim(), normalizedEmail, normalizedUsername, hashedPassword, paymentToken.trim());
        userRepository.save(rider);
        return new RegistrationResult(rider.getUserId(), rider.getUsername(), rider.getRole(), rider.getEmail(), rider.getFullName());
    }

    public record RegistrationResult(UUID userId, String username, String role, String email, String fullName) { }
}
