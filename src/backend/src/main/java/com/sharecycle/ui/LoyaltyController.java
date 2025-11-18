package com.sharecycle.ui;

import com.sharecycle.domain.model.LoyaltyTier;
import com.sharecycle.domain.repository.JpaLoyaltyRepository;
import com.sharecycle.application.CheckTierStatusUseCase;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.Map;

@RestController
@RequestMapping("/api/loyalty")
public class LoyaltyController {
    JpaLoyaltyRepository loyaltyRepository;
    CheckTierStatusUseCase checkTierStatusUseCase;

    public LoyaltyController(JpaLoyaltyRepository loyaltyRepository, CheckTierStatusUseCase checkTierStatusUseCase) {
        this.loyaltyRepository = loyaltyRepository;
        this.checkTierStatusUseCase = checkTierStatusUseCase;
    }

    @GetMapping("/status")
    public Map<String, Object> getLoyaltyStatus(@RequestParam UUID riderId) {
        LoyaltyTier tier = checkTierStatusUseCase.execute(riderId);
        return Map.of(
                "tier", tier.name(),
                "perks", getPerksDescription(tier));
    }

    private String getPerksDescription(LoyaltyTier tier) {
        return switch (tier) {
            case GOLD -> "15% discount on trips + 5-min extra hold";
            case SILVER -> "10% discount on trips + 2-min extra hold";
            case BRONZE -> "5% discount on trips";
            default -> "Standard rates apply";
        };
    }
}
