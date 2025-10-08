package com.sharecycle.service;

import org.springframework.security.crypto.bcrypt.BCrypt;

public class PasswordHasherImplement implements PasswordHasher {

    @Override
    public String hash(String password) {
        return "Hashed_" + password;
    }

        @Override
    public boolean verify(String password, String hash) {
        return BCrypt.checkpw(password, hash);
    }
}
