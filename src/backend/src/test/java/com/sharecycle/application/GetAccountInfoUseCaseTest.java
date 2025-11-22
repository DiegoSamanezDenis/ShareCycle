package com.sharecycle.application;

import com.sharecycle.domain.model.LoyaltyTier;
import com.sharecycle.domain.model.User;
import com.sharecycle.infrastructure.persistence.JpaUserRepository;
import com.sharecycle.model.dto.AccountInfoDto;
import com.sharecycle.service.LoyaltyEvaluatorService;
import com.sharecycle.ui.AccountController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class GetAccountInfoUseCaseTest {

    private LoyaltyEvaluatorService loyaltyEvaluatorService;
    private JpaUserRepository userRepository;
    private GetAccountInfoUseCase getAccountInfoUseCase;
    private AccountController accountController;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Mock dependencies
        loyaltyEvaluatorService = Mockito.mock(LoyaltyEvaluatorService.class);
        userRepository = Mockito.mock(JpaUserRepository.class);

        // Create the use case with both mocks
        getAccountInfoUseCase = new GetAccountInfoUseCase(loyaltyEvaluatorService, userRepository);
        accountController = new AccountController(getAccountInfoUseCase);

        // Create test user
        testUser = new User(
                UUID.randomUUID(),
                "Bhaskar",
                "concordia",
                "bh_das@example.com",
                "bhaskar_d",
                "",
                "USER",
                "",
                LocalDateTime.now(),
                LocalDateTime.now(),
                100.0
        );

        // Mock loyalty evaluation
        LoyaltyEvaluatorService.EvaluationResult evaluationResult =
                new LoyaltyEvaluatorService.EvaluationResult(LoyaltyTier.ENTRY, "Initial Tier");
        when(loyaltyEvaluatorService.evaluate(any(UUID.class), any(LoyaltyTier.class)))
                .thenReturn(evaluationResult);

        // Mock repository to return the user for flex credit
        when(userRepository.findById(testUser.getUserId())).thenReturn(testUser);
    }

    @Test
    void testGetAccount_returnsCorrectInfo() {
        // Execute the use case directly
        AccountInfoDto response = getAccountInfoUseCase.execute(testUser);

        // Verify the response
        assertThat(response).isNotNull();
        assertThat(response.fullName()).isEqualTo("Bhaskar");
        assertThat(response.email()).isEqualTo("bh_das@example.com");
        assertThat(response.username()).isEqualTo("bhaskar_d");
        assertThat(response.role()).isEqualTo("USER");
        assertThat(response.flexCredit()).isEqualTo(100.0); // Check flex credit
        assertThat(response.loyaltyTier()).isEqualTo(LoyaltyTier.ENTRY);
        assertThat(response.loyaltyReason()).isEqualTo("Initial Tier");
    }

}
