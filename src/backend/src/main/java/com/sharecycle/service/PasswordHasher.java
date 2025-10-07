package com.sharecycle.service;

public interface PasswordHasher {
    String hash(String password);
}
