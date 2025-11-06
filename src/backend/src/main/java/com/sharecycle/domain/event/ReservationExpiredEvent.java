package com.sharecycle.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

public class ReservationExpiredEvent implements DomainEvent {
    private final UUID reservationId;

    public ReservationExpiredEvent(UUID reservationId) {
        this.reservationId = reservationId;
    }

    public UUID getReservationId() {
        return reservationId;
    }

    @Override
    public LocalDateTime occurredAt() {
        return LocalDateTime.now();
    }
}
