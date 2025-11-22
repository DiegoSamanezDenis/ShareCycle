package com.sharecycle.ui;

import com.sharecycle.domain.event.*;
import com.sharecycle.domain.model.User;
import com.sharecycle.infrastructure.SimpleDomainEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class EventController {
    private static final Logger logger = LoggerFactory.getLogger(EventController.class);
    private static final long SSE_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(30);
    private final DomainEventPublisher publisher;
    private final SimpleDomainEventPublisher simplePublisher;

    public EventController(DomainEventPublisher publisher) {
        this.publisher = publisher;
        if (publisher instanceof SimpleDomainEventPublisher sp) {
            this.simplePublisher = sp;
        } else {
            this.simplePublisher = null;
        }
    }

    @GetMapping("/events")
    public List<String> recentEvents() {
        if (simplePublisher == null) return List.of();

        // 1. Identify the current user (Copy-paste from streamEvents)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (auth != null && auth.getPrincipal() instanceof User) ? (User) auth.getPrincipal() : null;
        UUID currentUserId = (currentUser != null) ? currentUser.getUserId() : null;
        boolean isOperator = (currentUser != null) && "OPERATOR".equals(currentUser.getRole());

        // 2. Filter the history list
        return simplePublisher.recentEvents().stream()
                .filter(e -> isRelevantToUser(e, currentUserId, isOperator)) // <--- This is the critical fix
                .map(EventController::format)
                .collect(Collectors.toList());
    }

    @GetMapping(path = "/events/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents() {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        // 1. Identify the current user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (auth != null && auth.getPrincipal() instanceof User) ? (User) auth.getPrincipal() : null;
        UUID currentUserId = (currentUser != null) ? currentUser.getUserId() : null;
        boolean isOperator = (currentUser != null) && "OPERATOR".equals(currentUser.getRole());

        DomainEventSubscriber subscriber = event -> {
            // 2. Filter: Only send relevant events
            if (!isRelevantToUser(event, currentUserId, isOperator)) {
                return;
            }

            try {
                emitter.send(SseEmitter.event()
                        .id(String.valueOf(event.occurredAt().toEpochSecond(java.time.ZoneOffset.UTC)))
                        .name(event.getClass().getSimpleName())
                        .data(format(event)));
            } catch (IOException ex) {
                logger.debug("Failed to send SSE event", ex);
            }
        };

        publisher.subscribe(subscriber);

        emitter.onCompletion(() -> publisher.unsubscribe(subscriber));
        emitter.onTimeout(() -> publisher.unsubscribe(subscriber));
        emitter.onError((ex) -> publisher.unsubscribe(subscriber));

        // 3. Send Initial Snapshot (Filtered)
        if (simplePublisher != null) {
            try {
                for (DomainEvent e : simplePublisher.recentEvents()) {
                    if (isRelevantToUser(e, currentUserId, isOperator)) {
                        emitter.send(SseEmitter.event()
                                .id(String.valueOf(e.occurredAt().toEpochSecond(java.time.ZoneOffset.UTC)))
                                .name(e.getClass().getSimpleName())
                                .data(format(e)));
                    }
                }
            } catch (IOException ignore) { }
        }

        return emitter;
    }

    /**
     * Filter logic to prevent users from seeing others' private notifications
     */
    private boolean isRelevantToUser(DomainEvent e, UUID userId, boolean isOperator) {
        // Operators see everything
        if (isOperator) return true;

        // Privacy Filter for Tier Updates
        if (e instanceof TierUpdatedEvent tue) {
            return userId != null && userId.equals(tue.riderId());
        }

        // Privacy Filter for Reservations
        if (e instanceof ReservationCreatedEvent rce) {
            return userId != null && userId.equals(rce.getRiderId());
        }

        // Privacy Filter for Flex Credit
        if (e instanceof FlexCreditAddedEvent fca) {
            return userId != null && userId.equals(fca.userId());
        }
        if (e instanceof FlexCreditDeductedEvent fcd) {
            return userId != null && userId.equals(fcd.userId());
        }

        // Default: Allow other events (TripStarted, StationStatus, etc.) to be visible
        return true;
    }

    private static String format(DomainEvent e) {
        var ts = e.occurredAt().format(DateTimeFormatter.ISO_LOCAL_TIME);
        return "%s - %s".formatted(ts, readable(e));
    }

    private static String readable(DomainEvent e) {
        if (e instanceof TripStartedEvent) return "Trip started.";
        if (e instanceof TripEndedEvent) return "Trip ended.";
        if (e instanceof TripBilledEvent) return "Trip billed.";
        if (e instanceof PaymentStartedEvent) return "Payment processing started.";
        if (e instanceof PaymentSucceedEvent) return "Payment completed successfully.";
        if (e instanceof PaymentFailedEvent) return "Payment failed.";
        if (e instanceof BillIssuedEvent event) return "Bill issued for trip %s totaling $%.2f".formatted(event.tripId().toString().substring(0, 8), event.totalCost());
        if (e instanceof ReservationCreatedEvent) return "Bike reserved.";
        if (e instanceof ReservationExpiredEvent) return "Reservation expired.";

        if (e instanceof BikeStatusChangedEvent event) {
            String bikeId = event.bikeId().toString().substring(0, 8);
            return "Bike " + bikeId + " status changed to " + event.status();
        }
        if (e instanceof StationStatusChangedEvent event) {
            String stationId = event.stationId().toString().substring(0, 8);
            return "Station " + stationId + " status changed to " + event.status();
        }
        if (e instanceof StationCapacityChangedEvent event) {
            String stationId = event.stationId().toString().substring(0, 8);
            return "Station " + stationId + " capacity changed to " + event.capacity() + " (Free docks: " + event.freeDocks() + ")";
        }
        if (e instanceof BikeMovedEvent event) {
            String bikeId = event.bikeId().toString().substring(0, 8);
            return "Bike " + bikeId + " has been moved from " + event.sourceStationId() + " to " + event.destinationStationId();
        }
        if (e instanceof FlexCreditAddedEvent event) {
            return "Flex credit added. Amount add: " + event.amount();
        }
        if (e instanceof FlexCreditDeductedEvent event) {
            return "Flex credit deducted. Amount deducted " +event.amount();
        }
        if (e instanceof TierUpdatedEvent event) {
            return String.format("Loyalty Tier updated to %s: %s", event.newTier(), event.reason());
        }

        return e.getClass().getSimpleName();
    }
}