package com.sharecycle.ui;

import com.sharecycle.application.BmsFacade;
import com.sharecycle.application.BmsFacade.TripCompletionResult;
import com.sharecycle.domain.model.Trip;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final BmsFacade bmsFacade;

    public TripController(BmsFacade bmsFacade) {
        this.bmsFacade = bmsFacade;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TripResponse startTrip(@RequestBody StartTripRequest request) {
        Trip trip = bmsFacade.startTrip(
                request.tripId(),
                request.riderId(),
                request.bikeId(),
                request.stationId(),
                request.startTime()
        );

        return new TripResponse(
                trip.getTripID(),
                trip.getStartStation().getId(),
                trip.getBike().getId(),
                trip.getRider().getUserId(),
                trip.getStartTime()
        );
    }

    @PostMapping("/{tripId}/end")
    public TripCompletionResponse endTrip(@PathVariable UUID tripId, @RequestBody EndTripRequest request) {
        TripCompletionResult result = bmsFacade.endTrip(tripId, request.stationId());
        Trip updatedTrip = result.trip();

        return new TripCompletionResponse(
                updatedTrip.getTripID(),
                updatedTrip.getEndStation().getId(),
                updatedTrip.getEndTime(),
                updatedTrip.getDurationMinutes(),
                result.ledgerEntry().getLedgerId(),
                result.ledgerEntry().getTotalAmount()
        );
    }

    public record StartTripRequest(
            UUID tripId,
            UUID riderId,
            UUID bikeId,
            UUID stationId,
            LocalDateTime startTime
    ) { }

    public record TripResponse(
            UUID tripId,
            UUID stationId,
            UUID bikeId,
            UUID riderId,
            LocalDateTime startedAt
    ) { }

    public record EndTripRequest(UUID stationId) { }

    public record TripCompletionResponse(
            UUID tripId,
            UUID endStationId,
            LocalDateTime endedAt,
            int durationMinutes,
            UUID ledgerId,
            double totalAmount
    ) { }
}
