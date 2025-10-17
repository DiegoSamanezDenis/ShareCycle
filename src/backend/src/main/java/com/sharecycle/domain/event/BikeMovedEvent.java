package com.sharecycle.domain.event;

import java.util.UUID;

public record BikeMovedEvent(UUID bikeId, UUID sourceStationId, UUID destinationStationId) {
}
