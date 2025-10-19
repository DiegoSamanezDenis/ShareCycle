package com.sharecycle.domain.model;

import java.util.UUID;

public class Dock {
    public enum DockStatus {
        EMPTY, OCCUPIED, OUT_OF_SERVICE
    }

    private UUID id;
    private DockStatus status;
    private Station station;
    private Bike occupiedBike;

    public Dock() {
        this(UUID.randomUUID(), DockStatus.EMPTY, null);
    }

    public Dock(UUID id, DockStatus status, Bike occupiedBike) {
        this.id = id == null ? UUID.randomUUID() : id;
        this.status = status;
        this.occupiedBike = occupiedBike;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public DockStatus getStatus() {
        return status;
    }

    public void setStatus(DockStatus status) {
        this.status = status;
    }

    public Station getStation() {
        return station;
    }

    public void setStation(Station station) {
        this.station = station;
        if (this.occupiedBike != null) {
            this.occupiedBike.setCurrentStation(station);
        }
    }

    public Bike getOccupiedBike() {
        return occupiedBike;
    }

    public void setOccupiedBike(Bike occupiedBike) {
        this.occupiedBike = occupiedBike;
        if (occupiedBike != null) {
            this.status = DockStatus.OCCUPIED;
            if (this.station != null) {
                occupiedBike.setCurrentStation(this.station);
                this.station.updateBikesDocked();
            }
        } else {
            this.status = DockStatus.EMPTY;
            if (this.station != null) {
                this.station.updateBikesDocked();
            }
        }
    }

    public boolean isEmpty() {
        return DockStatus.EMPTY.equals(this.status) && this.occupiedBike == null;
    }
}
