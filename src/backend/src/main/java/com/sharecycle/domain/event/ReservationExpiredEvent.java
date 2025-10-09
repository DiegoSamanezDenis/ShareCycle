package com.sharecycle.domain.event;

import java.util.UUID;

public class ReservationExpiredEvent {
    private final UUID reservationId;

    public ReservationExpiredEvent(UUID reservationId) {
        this.reservationId = reservationId;
    }

    public UUID getReservationId() {
        return reservationId;
    }
}
