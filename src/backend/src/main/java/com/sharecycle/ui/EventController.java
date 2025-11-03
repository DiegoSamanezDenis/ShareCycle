package com.sharecycle.ui;

import com.sharecycle.domain.event.DomainEvent;
import com.sharecycle.domain.event.DomainEventPublisher;
import com.sharecycle.domain.event.DomainEventSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
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
        var ts = e.occurredAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return "%s - %s".formatted(ts, readable(e));
    }

    private static String readable(DomainEvent e) {
        // Simple generic formatter: include class name and any known getter fields by toString.
        // You can customize per-event type for friendlier messages.
        return "%s %s".formatted(e.getClass().getSimpleName(), e.toString());
    }
}
