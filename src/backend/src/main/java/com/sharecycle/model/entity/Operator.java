package com.sharecycle.model.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@DiscriminatorValue("OPERATOR")
public class Operator extends User{

    public Operator() {
        super();
        //setRole("OPERATOR");
    }

    public Operator(String fullName, String streetAddress, String email, String username, String passwordHash, String paymentMethodToken) {
        super(fullName, streetAddress, email, username, passwordHash, paymentMethodToken, null, null);
    }
}
