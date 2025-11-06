package com.sharecycle.ui;

import java.time.LocalDateTime;
import java.util.UUID;
import com.sharecycle.application.BmsFacade;
import com.sharecycle.application.GetTripDetailsUseCase;
import com.sharecycle.application.BmsFacade.TripCompletionResult;
import com.sharecycle.domain.model.Trip;
import com.sharecycle.domain.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.sharecycle.application.BmsFacade;
import com.sharecycle.application.BmsFacade.TripCompletionResult;
import com.sharecycle.domain.model.Trip;

@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final BmsFacade bmsFacade;
    private final GetTripDetailsUseCase getTripDetailsUseCase;

    public TripController(BmsFacade bmsFacade, GetTripDetailsUseCase getTripDetailsUseCase) {
        this.bmsFacade = bmsFacade;
        this.getTripDetailsUseCase = getTripDetailsUseCase;
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

        var bill = result.ledgerEntry().getBill();
        return new TripCompletionResponse(
                updatedTrip.getTripID(),
                updatedTrip.getEndStation().getId(),
                updatedTrip.getEndTime(),
                updatedTrip.getDurationMinutes(),
                result.ledgerEntry().getLedgerId(),
                bill.getBaseCost(),
                bill.getTimeCost(),
                bill.getEBikeSurcharge(),
                bill.getTotalCost()
        );
    }

    @GetMapping("/{tripId}")
    public GetTripDetailsUseCase.TripDetails getTripDetails(@PathVariable UUID tripId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = null;
        if (auth != null && auth.getPrincipal() instanceof User user) {
                currentUser = user;
        } else {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return getTripDetailsUseCase.execute(tripId, currentUser);
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
            double baseCost,
            double timeCost,
            double eBikeSurcharge,
            double totalCost
    ) { }
}
