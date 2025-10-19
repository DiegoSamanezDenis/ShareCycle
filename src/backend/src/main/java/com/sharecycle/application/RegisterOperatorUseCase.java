package com.sharecycle.application;

import com.sharecycle.domain.repository.UserRepository;
import com.sharecycle.domain.model.Operator;
import com.sharecycle.domain.model.Operator;
import com.sharecycle.service.PasswordHasher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class RegisterOperatorUseCase {
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    //constructor
    @Autowired
    public RegisterOperatorUseCase(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    public Operator register(String fullName, String address, String email, String username, String password, String paymentToken) {
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

        //creating the operator (assign UUID explicitly; JPA requires manual id)
        Operator operator = new Operator(fullName, address, email, username, hashedpassword, paymentToken);
        operator.setUserId(UUID.randomUUID());

        userRepository.save(operator);

        // returning with password null so as to not expose it
        return new Operator(operator.getFullName(), operator.getStreetAddress(), operator.getEmail(), operator.getUsername(), null, null);

    }


}
