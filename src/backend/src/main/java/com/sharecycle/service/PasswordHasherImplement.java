package com.sharecycle.service;

public class PasswordHasherImplement implements PasswordHasher {

    @Override
    public String hash(String password) {
        return "Hashed_" + password;
    }
}
