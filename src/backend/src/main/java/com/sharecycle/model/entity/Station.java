package com.sharecycle.model.entity;

import jakarta.persistence.*;

import java.util.*;

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
        syncStatusFromCounts();
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
        for (Dock dock : docks) {
            dock.setStation(this);
        }
        recalculateCapacity();
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

    public boolean isOutOfService() {
        return StationStatus.OUT_OF_SERVICE.equals(status);
    }

    public boolean hasAvailableBike() {
        return bikesDocked > 0;
    }

    public boolean hasFreeDock() {
        return getFreeDockCount() > 0;
    }

    public void markOutOfService() {
        this.status = StationStatus.OUT_OF_SERVICE;
    }

    public int getFreeDockCount() {
        return capacity - bikesDocked;
    }

    public void addEmptyDocks(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Dock count to add cannot be negative.");
        }
        for (int i = 0; i < count; i++) {
            Dock dock = new Dock();
            dock.setStation(this);
            docks.add(dock);
        }
        recalculateCapacity();
    }

    public void removeEmptyDocks(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Dock count to remove cannot be negative.");
        }
        long emptyDockCount = docks.stream()
                .filter(Dock::isEmpty)
                .count();
        if (emptyDockCount < count) {
            throw new IllegalStateException("Not enough empty docks to remove.");
        }

        Iterator<Dock> iterator = docks.iterator();
        int removed = 0;
        while (iterator.hasNext() && removed < count) {
            Dock dock = iterator.next();
            if (dock.isEmpty()) {
                dock.setStation(null);
                iterator.remove();
                removed++;
            }
        }
        recalculateCapacity();
    }

    public Optional<Dock> findDockWithBike(UUID bikeId) {
        return docks.stream()
                .filter(dock -> dock.getOccupiedBike() != null && bikeId.equals(dock.getOccupiedBike().getId()))
                .findFirst();
    }

    public Optional<Dock> findFirstEmptyDock() {
        return docks.stream()
                .filter(Dock::isEmpty)
                .findFirst();
    }

    public void undockBike(Bike bike) {
        Dock dock = findDockWithBike(bike.getId())
                .orElseThrow(() -> new IllegalStateException("Bike is not docked at this station."));
        dock.setOccupiedBike(null);
    }

    public void dockBike(Bike bike) {
        Dock emptyDock = findFirstEmptyDock()
                .orElseThrow(() -> new IllegalStateException("Destination station has no free docks."));
        emptyDock.setOccupiedBike(bike);
    }

    private void syncStatusFromCounts() {
        if (!StationStatus.OUT_OF_SERVICE.equals(this.status)) {
            if (bikesDocked == 0) {
                this.status = StationStatus.EMPTY;
            } else if (bikesDocked >= capacity) {
                this.status = StationStatus.FULL;
            } else {
                this.status = StationStatus.OCCUPIED;
            }
        }
    }

    public void markActive() {
        this.status = StationStatus.EMPTY;
        syncStatusFromCounts();
    }

    private void recalculateCapacity() {
        this.capacity = docks.size();
        updateBikesDocked();
    }
    public Dock getFirstDockWithBike(){
        for (Dock dock : docks) {
            if (dock.getOccupiedBike() != null) {
                return dock;
            }
        }
        return null;
    }
}
