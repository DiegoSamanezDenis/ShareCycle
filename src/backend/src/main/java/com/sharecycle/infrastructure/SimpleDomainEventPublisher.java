package com.sharecycle.infrastructure;

import com.sharecycle.domain.event.DomainEventPublisher;

// import jakarta.persistence.criteria.CriteriaBuilder.In;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SimpleDomainEventPublisher implements DomainEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(SimpleDomainEventPublisher.class);
    private final DomainEventLog eventLog;
    private final InMemoryEventBuffer buffer;

    public SimpleDomainEventPublisher(DomainEventLog eventLog, InMemoryEventBuffer buffer) {
        this.eventLog = eventLog;
        this.buffer = buffer;
    }

    @Override
    public void publish(Object event) {
        logger.info("domain.event published name={}, payload={}", event.getClass().getSimpleName(), event);
        eventLog.append(event);
        try {
            buffer.append(event);
        } catch (Exception ex) {
            logger.warn("Failed to append event to in-memory buffer", ex);
        }
    }
}
