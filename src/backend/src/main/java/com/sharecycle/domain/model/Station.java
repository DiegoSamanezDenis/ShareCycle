package com.sharecycle.domain.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class Station {
    public enum StationStatus {
        EMPTY, OCCUPIED, FULL, OUT_OF_SERVICE
    }

    private UUID id;
    private String name;
    private StationStatus status;
    private double latitude;
    private double longitude;
    private int bikesDocked;
    private int capacity;
    private String address;
    private final List<Dock> docks = new ArrayList<>();

    public Station() {
        this(UUID.randomUUID(), "", StationStatus.EMPTY, 0.0, 0.0, "", 0, 0);
    }

    public Station(UUID id,
                   String name,
                   StationStatus status,
                   double latitude,
                   double longitude,
                   String address,
                   int capacity,
                   int bikesDocked) {
        this.id = id == null ? UUID.randomUUID() : id;
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

    public StationStatus getStatus() {
        return status;
    }

    public void setStatus(StationStatus status) {
        this.status = status;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
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

    public void setDocks(List<Dock> dockList) {
        docks.clear();
        if (dockList != null) {
            docks.addAll(dockList);
            for (Dock dock : docks) {
                dock.setStation(this);
            }
        }
        recalculateCapacity();
    }

    public void updateBikesDocked() {
        int totalOccupied = 0;
        for (Dock dock : docks) {
            if (dock.getStatus() == Dock.DockStatus.OCCUPIED) {
                totalOccupied++;
            }
        }
        setBikesDocked(totalOccupied);
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

    public void markActive() {
        this.status = StationStatus.EMPTY;
        syncStatusFromCounts();
    }

    public int getFreeDockCount() {
        return Math.max(0, capacity - bikesDocked);
    }

    public int getAvailableBikeCount() {
        int available = 0;
        for (Dock dock : docks) {
            if (dock.getOccupiedBike() != null && dock.getOccupiedBike().getStatus() == Bike.BikeStatus.AVAILABLE) {
                available++;
            }
        }
        return available;
    }

    public String getFullnessCategory() {
        if (capacity <= 0) {
            return "UNKNOWN";
        }
        double ratio = (double) bikesDocked / capacity;
        if (ratio == 0.0) {
            return "EMPTY";
        }
        if (ratio < 0.3) {
            return "LOW";
        }
        if (ratio > 0.9) {
            return "FULL";
        }
        return "HEALTHY";
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

    public Dock getFirstDockWithBike() {
        for (Dock dock : docks) {
            if (dock.getOccupiedBike() != null) {
                return dock;
            }
        }
        return null;
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

    private void recalculateCapacity() {
        this.capacity = docks.size();
        updateBikesDocked();
    }
}
