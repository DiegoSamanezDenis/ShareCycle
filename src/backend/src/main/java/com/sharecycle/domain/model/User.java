package com.sharecycle.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class User {
    private UUID userId;
    private String fullName;
    private String streetAddress;
    private String email;
    private String username;
    private String role;
    private String passwordHash;
    private String paymentMethodToken;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public User() {
    }

    public User(UUID userId,
                String fullName,
                String streetAddress,
                String email,
                String username,
                String passwordHash,
                String role,
                String paymentMethodToken,
                LocalDateTime createdAt,
                LocalDateTime updatedAt) {
        this.userId = userId;
        this.fullName = fullName;
        this.streetAddress = streetAddress;
        this.email = email;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = Objects.requireNonNull(role, "role must not be null");
        this.paymentMethodToken = paymentMethodToken;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getStreetAddress() {
        return streetAddress;
    }

    public void setStreetAddress(String streetAddress) {
        this.streetAddress = streetAddress;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = Objects.requireNonNull(role, "role must not be null");
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getPaymentMethodToken() {
        return paymentMethodToken;
    }

    public void setPaymentMethodToken(String paymentMethodToken) {
        this.paymentMethodToken = paymentMethodToken;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void touchOnCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void touchOnUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
