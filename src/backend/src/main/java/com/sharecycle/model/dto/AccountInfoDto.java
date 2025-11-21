package com.sharecycle.model.dto;

import com.sharecycle.domain.model.LoyaltyTier;

import java.util.UUID;

public record AccountInfoDto(
        UUID userId,
        String fullName,
        String email,
        String username,
        String role,
        double flexCredit,
        LoyaltyTier loyaltyTier,
        String loyaltyReason
) {}
