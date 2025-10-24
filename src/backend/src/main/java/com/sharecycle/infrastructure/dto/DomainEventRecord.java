package com.sharecycle.infrastructure.dto;

import java.time.Instant;

public record DomainEventRecord(String type, String payload, Instant occurredAt) {
}
