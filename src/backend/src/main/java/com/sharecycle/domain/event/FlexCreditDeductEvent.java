package com.sharecycle.domain.event;

import java.util.UUID;

public record FlexCreditDeductEvent(UUID userId, double amount) {
}
