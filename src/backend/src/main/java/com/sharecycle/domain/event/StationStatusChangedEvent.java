package com.sharecycle.domain.event;

import com.sharecycle.model.entity.Station;

import java.util.UUID;

public record StationStatusChangedEvent(UUID stationId, Station.StationStatus status, int capacity, int bikesDocked) {
}
