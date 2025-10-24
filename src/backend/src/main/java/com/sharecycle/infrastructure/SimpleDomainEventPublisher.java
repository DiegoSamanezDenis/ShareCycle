package com.sharecycle.infrastructure;

import com.sharecycle.domain.event.DomainEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SimpleDomainEventPublisher implements DomainEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(SimpleDomainEventPublisher.class);
    private final DomainEventLog eventLog;

    public SimpleDomainEventPublisher(DomainEventLog eventLog) {
        this.eventLog = eventLog;
    }

    @Override
    public void publish(Object event) {
        logger.info("domain.event published name={}, payload={}", event.getClass().getSimpleName(), event);
        eventLog.append(event);
    }
}
