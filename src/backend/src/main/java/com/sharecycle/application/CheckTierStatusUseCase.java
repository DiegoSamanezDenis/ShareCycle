package com.sharecycle.application;

import com.sharecycle.domain.event.DomainEventPublisher;
import com.sharecycle.domain.event.TierUpdatedEvent;
import com.sharecycle.domain.model.LoyaltyHistory;
import com.sharecycle.domain.model.LoyaltyTier;
import com.sharecycle.domain.repository.JpaLoyaltyRepository;
import com.sharecycle.service.LoyaltyEvaluatorService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class CheckTierStatusUseCase {
    private final LoyaltyEvaluatorService evaluatorService;
    private final JpaLoyaltyRepository loyaltyRepository;
    private final DomainEventPublisher eventPublisher;

    public CheckTierStatusUseCase(
            LoyaltyEvaluatorService evaluatorService,
            JpaLoyaltyRepository loyaltyRepository,
            DomainEventPublisher eventPublisher) {
        this.evaluatorService = evaluatorService;
        this.loyaltyRepository = loyaltyRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public LoyaltyTier execute(UUID riderId) {
        LoyaltyTier currentTier = loyaltyRepository.findCurrentTier(riderId);
        LoyaltyEvaluatorService.EvaluationResult result = evaluatorService.evaluate(riderId, currentTier);

        if (result.tier() != currentTier) {
            LoyaltyHistory history = new LoyaltyHistory(
                    UUID.randomUUID(),
                    riderId,
                    result.tier(),
                    LocalDateTime.now(),
                    result.reason()
            );
            loyaltyRepository.save(history);

            // Notify
            eventPublisher.publish(new TierUpdatedEvent(
                    riderId,
                    currentTier,
                    result.tier(),
                    result.reason()
            ));
        }

        return result.tier();
    }
}
