package com.sharecycle.domain.event;

import com.sharecycle.domain.model.Bill;

import java.util.UUID;

public record PaymentStartedEvent(UUID riderId, UUID tripId) {
}
