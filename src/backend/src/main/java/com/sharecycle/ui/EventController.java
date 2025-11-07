package com.sharecycle.ui;

import com.sharecycle.domain.event.DomainEvent;
import com.sharecycle.domain.event.DomainEventPublisher;
import com.sharecycle.domain.event.DomainEventSubscriber;
import com.sharecycle.domain.event.ReservationCreatedEvent;
import com.sharecycle.domain.event.ReservationExpiredEvent;
import com.sharecycle.domain.event.BikeStatusChangedEvent;
import com.sharecycle.domain.event.StationCapacityChangedEvent;
import com.sharecycle.domain.event.StationStatusChangedEvent;
import com.sharecycle.domain.event.TripBilledEvent;
import com.sharecycle.domain.event.TripEndedEvent;
import com.sharecycle.domain.event.TripStartedEvent;
import com.sharecycle.domain.event.BikeMovedEvent;
import com.sharecycle.domain.event.BillIssuedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class EventController {
    private static final Logger logger = LoggerFactory.getLogger(EventController.class);
    private static final long SSE_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(30);
    private final DomainEventPublisher publisher;
    private final com.sharecycle.infrastructure.SimpleDomainEventPublisher simplePublisher;

    public EventController(DomainEventPublisher publisher) {
        this.publisher = publisher;
        if (publisher instanceof com.sharecycle.infrastructure.SimpleDomainEventPublisher sp) {
            this.simplePublisher = sp;
        } else {
            this.simplePublisher = null;
        }
    }

    @GetMapping("/events")
    public List<String> recentEvents() {
        if (simplePublisher == null) return List.of();
        return simplePublisher.recentEvents().stream()
                .map(EventController::format)
                .collect(Collectors.toList());
    }

    @GetMapping(path = "/events/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents() {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        DomainEventSubscriber subscriber = event -> {
            try {
                emitter.send(SseEmitter.event()
                        .id(String.valueOf(event.occurredAt().toEpochSecond(java.time.ZoneOffset.UTC)))
                        .name(event.getClass().getSimpleName())
                        .data(format(event)));
            } catch (IOException ex) {
                // emitter likely closed; ignore and let lifecycle handle unsubscribe
                logger.debug("Failed to send SSE event", ex);
            }
        };

        // subscribe
        publisher.subscribe(subscriber);

        // when emitter completes / times out / error -> unsubscribe
        emitter.onCompletion(() -> publisher.unsubscribe(subscriber));
        emitter.onTimeout(() -> publisher.unsubscribe(subscriber));
        emitter.onError((ex) -> publisher.unsubscribe(subscriber));

        // send initial snapshot
        if (simplePublisher != null) {
            try {
                for (DomainEvent e : simplePublisher.recentEvents()) {
                    emitter.send(SseEmitter.event()
                            .id(String.valueOf(e.occurredAt().toEpochSecond(java.time.ZoneOffset.UTC)))
                            .name(e.getClass().getSimpleName())
                            .data(format(e)));
                }
            } catch (IOException ignore) { /* best-effort */ }
        }

        return emitter;
    }

    private static String format(DomainEvent e) {
        var ts = e.occurredAt().format(DateTimeFormatter.ISO_LOCAL_TIME);
        return "%s - %s".formatted(ts, readable(e));
    }

    private static String readable(DomainEvent e) {
        if (e instanceof TripStartedEvent) {
            return "Trip started.";
        }
        if (e instanceof TripEndedEvent) {
            return "Trip ended.";
        }
        if (e instanceof TripBilledEvent) {
            return "Trip billed.";
        }
        if (e instanceof BillIssuedEvent event) {
            return "Bill issued for trip %s totaling $%.2f".formatted(
                    event.tripId().toString().substring(0, 8),
                    event.totalCost()
            );
        }
        if (e instanceof ReservationCreatedEvent) {
            return "Bike reserved.";
        }
        if (e instanceof ReservationExpiredEvent) {
            return "Reservation expired.";
        }

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

        return e.getClass().getSimpleName();
    }
}
