package com.sharecycle.domain.event;

import java.util.UUID;

public record StationCapacityChangedEvent(
        UUID stationId,
        int capacity,
        int bikesDocked,
        int freeDocks
) {
}
