package com.sharecycle.service;

import jakarta.websocket.server.ServerEndpoint;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

@Service
public class BcryptHasher implements PasswordHasher{

    @Override
    public String hash(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }
    
    @Override
    public boolean verify(String password, String hash) {
        return BCrypt.checkpw(password, hash);
    }
}


