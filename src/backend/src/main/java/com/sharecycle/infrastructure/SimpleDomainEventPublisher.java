package com.sharecycle.infrastructure;

import com.sharecycle.domain.event.DomainEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SimpleDomainEventPublisher implements DomainEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(SimpleDomainEventPublisher.class);

    @Override
    public void publish(Object event) {
        logger.info("Event published: {}", event.getClass().getSimpleName());
    }
}
