package com.sharecycle.infrastructure;

import com.sharecycle.infrastructure.dto.DomainEventRecord;
import com.sharecycle.infrastructure.persistence.JpaDomainEventRepository;
import com.sharecycle.infrastructure.persistence.jpa.JpaDomainEventEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DomainEventLog {

    private static final int DEFAULT_LIMIT = 100;

    private final JpaDomainEventRepository repository;

    public DomainEventLog(JpaDomainEventRepository repository) {
        this.repository = repository;
    }

    public void append(Object event) {
        if (event == null) {
            return;
        }
        repository.save(JpaDomainEventEntity.fromEvent(event));
    }

    public List<DomainEventRecord> latest(int limit) {
        int effectiveLimit = limit > 0 ? limit : DEFAULT_LIMIT;
        return repository.findRecent(effectiveLimit).stream()
                .map(entity -> new DomainEventRecord(
                        entity.getEventType(),
                        entity.getMessage(),
                        entity.getOccurredAt()
                ))
                .toList();
    }
}
