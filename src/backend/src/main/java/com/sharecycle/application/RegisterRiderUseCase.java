package com.sharecycle.application;

import com.sharecycle.domain.repository.UserRepository;
import com.sharecycle.model.entity.Rider;
import com.sharecycle.service.PasswordHasher;

public class RegisterRiderUseCase {
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    //constructor
    public RegisterRiderUseCase(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    public Rider register(String fullName,String address,  String email, String username, String password, String paymentToken) {
        //validate email
        if(email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Email address is invalid");
        }

        //check is username and password not set (require fields)
        if (username==null || username.isEmpty() || password==null || password.isEmpty()) {
            throw new IllegalArgumentException("Username and password is required");
        }

        //check if the user already exist
        if (userRepository.existsByEmail(email) || userRepository.existsByUsername(username)){
            throw new IllegalArgumentException("Username/ email is already in use");
        }

        if (password.length()>72){
            throw new IllegalArgumentException("Password is too long");
        }
        //hashing the password
        String hashedpassword = passwordHasher.hash(password);

        //creating the rider
        Rider rider = new Rider(fullName, address, email, username, hashedpassword, paymentToken);

        userRepository.save(rider);

        // reutrning with password null so as to not expose it
        return new Rider(rider.getFullName(), rider.getStreetAddress(), rider.getEmail(), rider.getUsername(), null, rider.getPaymentMethodToken());

    }


}
