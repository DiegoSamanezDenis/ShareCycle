package com.sharecycle.infrastructure;

import com.sharecycle.infrastructure.dto.DomainEventRecord;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

@Component
public class InMemoryEventBuffer {
    private final int capacity;
    private final Deque<DomainEventRecord> buffer;
    public InMemoryEventBuffer() {
        this(200);
    }
    public InMemoryEventBuffer(int capacity) {
        this.capacity = capacity;
        this.buffer = new ArrayDeque<>(capacity);
    }
    public synchronized void append(Object event) {
        if (event == null) return;
        String type = event.getClass().getSimpleName();
        String payload = event.toString();
        DomainEventRecord rec = new DomainEventRecord(type, payload, Instant.now());
        if (buffer.size() >= capacity) {
            buffer.removeFirst();
        }
        buffer.addLast(rec);
    }
    public synchronized List<DomainEventRecord> latest(int limit) {
        int effective = Math.min(limit <= 0 ? 100 : limit, capacity);
        List<DomainEventRecord> out = new ArrayList<>(effective);
        var it = buffer.descendingIterator();
        while (it.hasNext() && out.size() < effective) {
            out.add(it.next());
        }
        return out;
    }
    
}
