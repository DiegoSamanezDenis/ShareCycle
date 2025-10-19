package com.sharecycle.ui;

import com.sharecycle.application.LoginUseCase;
import com.sharecycle.service.SessionStore;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class LoginController {
    private final LoginUseCase loginUseCase;
    private final SessionStore sessionStore;

    public LoginController(LoginUseCase loginUseCase, SessionStore sessionStore){
        this.loginUseCase = loginUseCase;
        this.sessionStore = sessionStore;
    }

    @PostMapping("/login")
    public LoginUseCase.LoginResponse login(@RequestBody LoginRequest request){
        return loginUseCase.execute(request.username, request.password);
    }

    @PostMapping("/logout")
    public void logout(@RequestHeader("Authorization") String token) {
        sessionStore.invalidate(token);
    }

    public record LoginRequest(String username, String password){}
}
