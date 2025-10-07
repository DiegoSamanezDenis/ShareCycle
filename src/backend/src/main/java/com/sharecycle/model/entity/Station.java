package com.sharecycle.model.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
public class Station {
    public enum StationStatus {
        EMPTY, OCCUPIED, FULL, OUT_OF_SERVICE,
    }

    @Id
    @Column(name = "station_id", columnDefinition = "BINARY(16)", unique = true, nullable = false)
    private UUID id;

    @Column(name = "station_name", nullable = true)
    private String name;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "station_status", nullable = false)
    private StationStatus status;

    @Column(name = "station_latitude", nullable = false)
    private double latitude;

    @Column(name = "station_longtitude", nullable = false)
    private double longitude;

    @Column(name = "bikes_docked", nullable = false)
    private int bikesDocked;

    @Column(name = "station_capacity", nullable = false)
    private int capacity;

    @Column(name = "address", nullable = false)
    private String address; // Keep it simple, no need libaddressinput

    @OneToMany(mappedBy = "station", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Dock> docks;

    public Station() {
        this.id = UUID.randomUUID();
        this.status = StationStatus.EMPTY;
        this.latitude = 0.0;
        this.longitude = 0.0;
        this.bikesDocked = 0;
        this.capacity = 0;
        this.address = "";
        this.docks = new ArrayList<>();
    }

    public Station(String name, StationStatus status, double latitude, double longitude, String address, int capacity, int bikesDocked) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.status = status;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.capacity = capacity;
        this.bikesDocked = bikesDocked;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public StationStatus getStatus() {
        return status;
    }

    public void setStatus(StationStatus status) {
        this.status = status;
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public List<Dock> getDocks() {
        return docks;
    }

    public void setDocks(List<Dock> docks) {
        this.docks = docks;
        updateBikesDocked();
    }

    public void updateBikesDocked() {
        int totalOccupied = 0;
        for (Dock dock : docks) {
            if (dock.getStatus() == Dock.DockStatus.OCCUPIED){
                totalOccupied++;
            }
        }
        this.setBikesDocked(totalOccupied);
    }
}
