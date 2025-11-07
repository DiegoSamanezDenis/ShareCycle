package com.sharecycle.application;

import com.sharecycle.domain.model.PricingPlan;
import com.sharecycle.domain.model.Rider;
import com.sharecycle.domain.repository.UserRepository;
import com.sharecycle.service.PasswordHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RegisterRiderUseCaseTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordHasher passwordHasher;

    private RegisterRiderUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RegisterRiderUseCase(userRepository, passwordHasher);
    }

    @Test
    void registerPersistsTrimmedLowerCasedEmailAndHashedPassword() {
        doReturn(false).when(userRepository).existsByEmail(any());
        doReturn(false).when(userRepository).existsByUsername(any());
        doReturn("hashedPass").when(passwordHasher).hash("secret123");

        useCase.register(
                "  Jane Rider  ",
                "  123 Street ",
                "Jane.Rider@Example.com ",
                "  JaneUser ",
                "secret123",
                " pm_tok ",
                "PAY_AS_YOU_GO"
        );

        ArgumentCaptor<Rider> riderCaptor = ArgumentCaptor.forClass(Rider.class);
        verify(userRepository).save(riderCaptor.capture());
        Rider rider = riderCaptor.getValue();

        assertThat(rider.getEmail()).isEqualTo("jane.rider@example.com");
        assertThat(rider.getUsername()).isEqualTo("JaneUser");
        assertThat(rider.getPasswordHash()).isEqualTo("hashedPass");
        assertThat(rider.getPaymentMethodToken()).isEqualTo("pm_tok");
        assertThat(rider.getPricingPlanType()).isEqualTo(PricingPlan.PlanType.PAY_AS_YOU_GO);
    }

    @Test
    void registerRejectsInvalidEmail() {
        assertThatThrownBy(() -> useCase.register(
                "Name", "Addr", "not-an-email", "user", "secret123", "tok", "PAY_AS_YOU_GO"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email address is invalid");
    }

    @Test
    void registerRejectsDuplicateUsernameOrEmail() {
        doReturn(true).when(userRepository).existsByEmail("jane@example.com");

        assertThatThrownBy(() -> useCase.register(
                "Name", "Addr", "jane@example.com", "user", "secret123", "tok", "PAY_AS_YOU_GO"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already in use");
    }
}
