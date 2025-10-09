package com.sharecycle.model.dto;


public class StationSummaryDto {
    private String name;
    private int bikesDocked;
    private int capacity;
    public StationSummaryDto(String name, int bikesDocked, int capacity) {
        this.name = name;
        this.bikesDocked = bikesDocked;
        this.capacity = capacity;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getBikesDocked() {
        return bikesDocked;
    }

    public void setBikesDocked(int bikesDocked) {
        this.bikesDocked = bikesDocked;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }
}
