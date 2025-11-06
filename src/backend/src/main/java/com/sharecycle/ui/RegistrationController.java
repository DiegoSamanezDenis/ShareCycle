package com.sharecycle.ui;

import com.sharecycle.application.RegisterRiderUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class RegistrationController {

    private final RegisterRiderUseCase registerRiderUseCase;

    public RegistrationController(RegisterRiderUseCase registerRiderUseCase) {
        this.registerRiderUseCase = registerRiderUseCase;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterRiderUseCase.RegistrationResult register(@RequestBody RegisterRequest request) {
        return registerRiderUseCase.register(
                request.fullName(),
                request.streetAddress(),
                request.email(),
                request.username(),
                request.password(),
                request.paymentMethodToken(),
                request.pricingPlanType()
        );
    }

    public record RegisterRequest(
            String fullName,
            String streetAddress,
            String email,
            String username,
            String password,
            String paymentMethodToken,
            String pricingPlanType
    ) { }
}
