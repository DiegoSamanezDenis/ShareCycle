package com.sharecycle.domain.model;

import java.util.UUID;

public class Rider extends User {

    public Rider() {
        super.setRole("RIDER");
    }

    public Rider(User user) {
        super(user.getUserId(),
                user.getFullName(),
                user.getStreetAddress(),
                user.getEmail(),
                user.getUsername(),
                user.getPasswordHash(),
                "RIDER",
                user.getPaymentMethodToken(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }

    public Rider(String fullName,
                 String streetAddress,
                 String email,
                 String username,
                 String passwordHash,
                 String paymentMethodToken) {
        super(UUID.randomUUID(),
                fullName,
                streetAddress,
                email,
                username,
                passwordHash,
                "RIDER",
                paymentMethodToken,
                null,
                null);
    }
}
