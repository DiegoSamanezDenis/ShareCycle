package com.sharecycle.application;

import java.util.UUID;

/**
 * Exception raised when a rider attempts to return a bike to a station that has no free docks.
 * Carries the identifier of the station so downstream layers can present helpful context.
 */
public class StationFullException extends RuntimeException {

    private final UUID stationId;

    public StationFullException(UUID stationId, String message) {
        super(message);
        this.stationId = stationId;
    }

    public StationFullException(UUID stationId) {
        this(stationId, "Destination station has no free docks.");
    }

    public UUID getStationId() {
        return stationId;
    }
}

