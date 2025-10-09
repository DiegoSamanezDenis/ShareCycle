package com.sharecycle.domain.event;

public interface DomainEventPublisher {
    void publish(Object event);
}

