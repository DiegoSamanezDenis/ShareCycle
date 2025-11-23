package com.sharecycle.ui;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.sharecycle.application.LoginUseCase;
import com.sharecycle.application.ToggleRoleUseCase;
import com.sharecycle.service.SessionStore;

@RestController
@RequestMapping("/api/auth")
public class LoginController {
    private final LoginUseCase loginUseCase;
    private final SessionStore sessionStore;
    private final ToggleRoleUseCase toggleRoleUseCase;

    public LoginController(LoginUseCase loginUseCase, SessionStore sessionStore, ToggleRoleUseCase toggleRoleUseCase){
        this.loginUseCase = loginUseCase;
        this.sessionStore = sessionStore;
        this.toggleRoleUseCase = toggleRoleUseCase;
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

    @PostMapping("/toggle-role")
    public ToggleRoleUseCase.ToggleRoleResponse toggleRole(
            @RequestHeader(value = "Authorization", required = true) String token) {
        String resolved = resolveToken(token);
        if (resolved == null) {
            throw new IllegalArgumentException("Authorization token is required");
        }
        return toggleRoleUseCase.execute(resolved);
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
