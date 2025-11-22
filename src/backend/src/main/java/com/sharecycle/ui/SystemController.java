package com.sharecycle.ui;

import com.sharecycle.application.ResetSystemUseCase;
import com.sharecycle.domain.model.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    private final ResetSystemUseCase resetSystemUseCase;

    public SystemController(ResetSystemUseCase resetSystemUseCase) {
        this.resetSystemUseCase = resetSystemUseCase;
    }

    @PostMapping("/reset")
    public ResetResponse resetSystem() {
        User operator = requireOperator();
        ResetSystemUseCase.ResetSummary summary = resetSystemUseCase.execute(operator.getUserId());
        return new ResetResponse(summary.bikes(), summary.stations(), summary.docks());
    }

    private User requireOperator() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required.");
        }
        String role = user.getRole() != null ? user.getRole().toUpperCase() : "";
        if (!"OPERATOR".equals(role) && !"ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Operator role required.");
        }
        return user;
    }

    public record ResetResponse(int bikes, int stations, int docks) {}
}

