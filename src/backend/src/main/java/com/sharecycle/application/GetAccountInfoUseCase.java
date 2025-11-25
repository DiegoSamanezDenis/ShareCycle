package com.sharecycle.application;

import com.sharecycle.domain.model.User;
import com.sharecycle.domain.model.LoyaltyTier;
import com.sharecycle.model.dto.AccountInfoDto;
import com.sharecycle.service.LoyaltyEvaluatorService;
import com.sharecycle.infrastructure.persistence.JpaUserRepository;
import org.springframework.stereotype.Service;

@Service
public class GetAccountInfoUseCase {

    private final LoyaltyEvaluatorService loyaltyEvaluatorService;
    private final JpaUserRepository userRepository;

    public GetAccountInfoUseCase(LoyaltyEvaluatorService loyaltyEvaluatorService,
                                 JpaUserRepository userRepository) {
        this.loyaltyEvaluatorService = loyaltyEvaluatorService;
        this.userRepository = userRepository;
    }

    public AccountInfoDto execute(User user) {
        // Fetch the latest user state (to get updated flex credit)
        User freshUser = userRepository.findById(user.getUserId());

        // Evaluate loyalty tier
        LoyaltyEvaluatorService.EvaluationResult result =
                loyaltyEvaluatorService.evaluate(freshUser.getUserId(), LoyaltyTier.ENTRY);

        return new AccountInfoDto(
                freshUser.getUserId(),
                freshUser.getFullName(),
                freshUser.getEmail(),
                freshUser.getUsername(),
                freshUser.getRole(),
                freshUser.getFlexCredit(), // latest flex credit
                result.tier(),
                result.reason()
        );
    }
}
