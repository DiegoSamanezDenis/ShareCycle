package com.sharecycle.domain.event;

public interface DomainEventSubscriber {
    void onEvent(DomainEvent event);
}
