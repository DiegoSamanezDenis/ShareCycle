package com.sharecycle.infrastructure;

import com.sharecycle.domain.event.DomainEvent;
import com.sharecycle.domain.event.DomainEventPublisher;
import com.sharecycle.domain.event.DomainEventSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Keeps a small in-memory ring buffer of the last N events and notifies subscribers.
 */
@Component
public class SimpleDomainEventPublisher implements DomainEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(SimpleDomainEventPublisher.class);
    private static final int DEFAULT_CAPACITY = 200;

    private final Deque<DomainEvent> ring = new ConcurrentLinkedDeque<>();
    private final List<DomainEventSubscriber> subscribers = new CopyOnWriteArrayList<>();
    private final int capacity;

    public SimpleDomainEventPublisher() {
        this(DEFAULT_CAPACITY);
    }

    public SimpleDomainEventPublisher(int capacity) {
        this.capacity = Math.max(16, capacity);
    }

    @Override
    public void subscribe(DomainEventSubscriber subscriber) {
        if (subscriber != null) {
            subscribers.add(subscriber);
        }
    }

    @Override
    public void unsubscribe(DomainEventSubscriber subscriber) {
        subscribers.remove(subscriber);
    }

    @Override
    public void publish(DomainEvent event) {
        if (event == null) return;
        logger.info("publishing domain event: {}", event.getClass().getSimpleName());

        // append to ring buffer
        ring.addLast(event);
        while (ring.size() > capacity) {
            ring.pollFirst();
        }

        // notify subscribers, isolating failures
        for (DomainEventSubscriber s : subscribers) {
            try {
                s.onEvent(event);
            } catch (Exception ex) {
                logger.warn("subscriber failed handling event {}", event.getClass().getSimpleName(), ex);
            }
        }
    }

    /**
     * Return a snapshot of recent events (newest first).
     */
    public List<DomainEvent> recentEvents() {
        return ring.stream()
                .collect(Collectors.toList())
                .stream()
                .sorted((a, b) -> b.occurredAt().compareTo(a.occurredAt()))
                .collect(Collectors.toList());
    }
}