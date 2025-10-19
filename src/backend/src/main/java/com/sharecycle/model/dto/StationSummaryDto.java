package com.sharecycle.model.dto;

import com.sharecycle.domain.model.Station;

import java.util.UUID;

public class StationSummaryDto {
    private final UUID stationId;
    private final String name;
    private final Station.StationStatus status;
    private final int bikesDocked;
    private final int capacity;
    private final int freeDocks;

    public StationSummaryDto(UUID stationId, String name, Station.StationStatus status, int bikesDocked, int capacity, int freeDocks) {
        this.stationId = stationId;
        this.name = name;
        this.status = status;
        this.bikesDocked = bikesDocked;
        this.capacity = capacity;
        this.freeDocks = freeDocks;
    }

    public UUID getStationId() {
        return stationId;
    }

    public String getName() {
        return name;
    }

    public Station.StationStatus getStatus() {
        return status;
    }

    public int getBikesDocked() {
        return bikesDocked;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getFreeDocks() {
        return freeDocks;
    }
}
