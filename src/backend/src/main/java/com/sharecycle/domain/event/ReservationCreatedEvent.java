package com.sharecycle.domain.event;

import java.time.LocalDateTime;

public class ReservationCreatedEvent implements DomainEvent {
    private java.util.UUID reservationId;
    private java.util.UUID riderId;

    public ReservationCreatedEvent(java.util.UUID reservationId, java.util.UUID riderId) {
        this.reservationId = reservationId;
        this.riderId = riderId;
    }

    public java.util.UUID getReservationId() {
        return reservationId;
    }

    public java.util.UUID getRiderId() {
        return riderId;
    }

    @Override
    public LocalDateTime occurredAt() {
        return LocalDateTime.now();
    }
}
