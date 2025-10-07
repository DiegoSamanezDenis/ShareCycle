package com.sharecycle.service;

import org.springframework.security.crypto.bcrypt.BCrypt;

public class BcryptHasher implements PasswordHasher{

    @Override
    public String hash(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }
}


