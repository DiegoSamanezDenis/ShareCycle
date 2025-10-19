package com.sharecycle.domain.model;

public class Operator extends User {

    public Operator() {
        super.setRole("OPERATOR");
    }

    public Operator(User user) {
        super(user.getUserId(),
                user.getFullName(),
                user.getStreetAddress(),
                user.getEmail(),
                user.getUsername(),
                user.getPasswordHash(),
                "OPERATOR",
                user.getPaymentMethodToken(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }

    public Operator(String fullName,
                    String streetAddress,
                    String email,
                    String username,
                    String passwordHash,
                    String paymentMethodToken) {
        super(null,
                fullName,
                streetAddress,
                email,
                username,
                passwordHash,
                "OPERATOR",
                paymentMethodToken,
                null,
                null);
    }
}
