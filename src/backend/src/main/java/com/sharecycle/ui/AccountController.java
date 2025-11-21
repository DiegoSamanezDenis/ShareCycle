package com.sharecycle.ui;

import com.sharecycle.application.GetAccountInfoUseCase;
import com.sharecycle.model.dto.AccountInfoDto;
import com.sharecycle.domain.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AccountController {

    private final GetAccountInfoUseCase getAccountInfoUseCase;

    public AccountController(GetAccountInfoUseCase getAccountInfoUseCase) {
        this.getAccountInfoUseCase = getAccountInfoUseCase;
    }

    @GetMapping("/api/account")
    public AccountInfoDto getAccountInfo() {
        // Fetch the authenticated user from Spring Security
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User is not authenticated");
        }

        // Assuming your UserDetails implementation returns your domain User
        User currentUser = (User) authentication.getPrincipal();

        return getAccountInfoUseCase.execute(currentUser);
    }
}
