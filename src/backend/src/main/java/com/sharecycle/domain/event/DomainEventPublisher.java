package com.sharecycle.domain.event;

public interface DomainEventPublisher {
    void subscribe(DomainEventSubscriber subscriber);
    void unsubscribe(DomainEventSubscriber subscriber);
    void publish(Object event);

    void publish(DomainEvent event);
}

