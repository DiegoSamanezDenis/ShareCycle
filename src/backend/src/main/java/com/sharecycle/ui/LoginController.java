package com.sharecycle.ui;

import com.sharecycle.application.LoginUseCase;
import com.sharecycle.service.SessionStore;
import org.springframework.http.HttpStatus;
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
        return loginUseCase.execute(request.username(), request.password());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestHeader(value = "Authorization", required = false) String token) {
        String resolved = resolveToken(token);
        if (resolved != null) {
            sessionStore.invalidate(resolved);
        }
    }

    private String resolveToken(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        String value = header.trim();
        if (value.startsWith("Bearer ")) {
            value = value.substring(7);
        }
        return value.isBlank() ? null : value;
    }

    public record LoginRequest(String username, String password){}
}
