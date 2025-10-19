package com.sharecycle.ui;

import com.sharecycle.application.BmsFacade;
import com.sharecycle.domain.model.Reservation;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final BmsFacade bmsFacade;

    public ReservationController(BmsFacade bmsFacade) {
        this.bmsFacade = bmsFacade;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse reserve(@RequestBody ReservationRequest request) {
        Reservation reservation = bmsFacade.reserveBike(
                request.riderId(),
                request.stationId(),
                request.bikeId(),
                request.expiresAfterMinutes()
        );

        return new ReservationResponse(
                reservation.getReservationId(),
                reservation.getStation().getId(),
                reservation.getBike().getId(),
                reservation.getReservedAt(),
                reservation.getExpiresAt(),
                reservation.isActive()
        );
    }

    public record ReservationRequest(
            UUID riderId,
            UUID stationId,
            UUID bikeId,
            int expiresAfterMinutes
    ) { }

    public record ReservationResponse(
            UUID reservationId,
            UUID stationId,
            UUID bikeId,
            Instant reservedAt,
            Instant expiresAt,
            boolean active
    ) { }
}
