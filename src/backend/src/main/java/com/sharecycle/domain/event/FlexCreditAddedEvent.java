package com.sharecycle.domain.event;

import java.util.UUID;

public record FlexCreditAddedEvent(UUID userId, double amountAdded) {
}
