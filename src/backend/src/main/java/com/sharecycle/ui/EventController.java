package com.sharecycle.ui;

import com.sharecycle.infrastructure.InMemoryEventBuffer;
import com.sharecycle.infrastructure.dto.DomainEventRecord;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/events")
public class EventController {

    private final InMemoryEventBuffer buffer;

    public EventController(InMemoryEventBuffer buffer) {
        this.buffer = buffer;
    }

    @GetMapping("/latest")
    public List<DomainEventRecord> latest(@RequestParam(name = "limit", required = false, defaultValue = "50") int limit) {
        int max = Math.min(limit, 200);
        return buffer.latest(max);
    }
}
