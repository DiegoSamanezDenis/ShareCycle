package com.sharecycle.ui;

import com.sharecycle.infrastructure.DomainEventLog;
import com.sharecycle.infrastructure.dto.DomainEventRecord;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/events")
public class EventController {

    private final DomainEventLog eventLog;

    public EventController(DomainEventLog eventLog) {
        this.eventLog = eventLog;
    }

    @GetMapping
    public List<DomainEventRecord> recentEvents() {
        return eventLog.latest(50);
    }
}
