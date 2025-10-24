package com.sharecycle.model.dto;

import com.sharecycle.domain.model.Station;

import java.util.UUID;

public class StationSummaryDto {
    private final UUID stationId;
    private final String name;
    private final Station.StationStatus status;
    private final int bikesAvailable;
    private final int bikesDocked;
    private final int capacity;
    private final int freeDocks;
    private final double latitude;
    private final double longitude;
    private final String fullnessCategory;

    public StationSummaryDto(UUID stationId,
                             String name,
                             Station.StationStatus status,
                             int bikesAvailable,
                             int bikesDocked,
                             int capacity,
                             int freeDocks,
                             double latitude,
                             double longitude,
                             String fullnessCategory) {
        this.stationId = stationId;
        this.name = name;
        this.status = status;
        this.bikesAvailable = bikesAvailable;
        this.bikesDocked = bikesDocked;
        this.capacity = capacity;
        this.freeDocks = freeDocks;
        this.latitude = latitude;
        this.longitude = longitude;
        this.fullnessCategory = fullnessCategory;
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

    public int getBikesAvailable() {
        return bikesAvailable;
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

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getFullnessCategory() {
        return fullnessCategory;
    }
}
