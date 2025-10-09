package com.sharecycle.application;

import com.sharecycle.domain.repository.UserRepository;
import com.sharecycle.model.entity.User;
import com.sharecycle.service.PasswordHasher;
import com.sharecycle.service.SessionStore;

import java.util.Optional;

public class LoginUseCase {
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final SessionStore sessionStore;

    public LoginUseCase (UserRepository userRepository, PasswordHasher passwordHasher, SessionStore sessionStore){
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.sessionStore = sessionStore;
    }

    public LoginResponse execute(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()){
            throw new RuntimeException("Invalid username or password");
        }
        User user = userOpt.get();
        if (!passwordHasher.verify(password, user.getPasswordHash())){
            throw new RuntimeException("Invalid username or password");
        }
        String token = sessionStore.createSession(user.getUserId());
        return new LoginResponse(token, user.getRole());
    }


    public record LoginResponse(String token, String role){}
}