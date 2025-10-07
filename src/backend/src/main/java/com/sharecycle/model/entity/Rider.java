package com.sharecycle.model.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@DiscriminatorValue("RIDER")
public class Rider extends User{

    //default constructor for jpa
public Rider() {
    super();
   // setRole("RIDER"); // here the role is being set for the discriminator
}

    public Rider(String fullName, String streetAddress, String email, String username, String passwordHash, String paymentMethodToken) {
        super(fullName, streetAddress, email, username, passwordHash, paymentMethodToken, null, null);
    }
}
