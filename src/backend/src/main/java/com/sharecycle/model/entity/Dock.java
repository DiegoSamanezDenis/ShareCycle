package com.sharecycle.model.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
public class Dock {
    public enum DockStatus{
        EMPTY, OCCUPIED, OUT_OF_SERVICE,
    }
    @Id
    @Column(name = "dock_id", unique = true, nullable = false)
    private UUID id;


    @Enumerated(EnumType.ORDINAL)
    @Column(name = "dock_status", nullable = false)
    private DockStatus status;

    @ManyToOne
    @JoinColumn(name = "station_id")
    private Station station;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "bike_id")
    private Bike occupiedBike;

    public Dock() {
        this.id = UUID.randomUUID();
        this.status = DockStatus.EMPTY;
        this.occupiedBike = null;
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

    public Station getStation() {
        return station;
    }

    public void setStation(Station station) {
        this.station = station;
        if (this.occupiedBike != null) {
            this.occupiedBike.setCurrentStation(station);
        }
    }

    public boolean isEmpty() {
        return DockStatus.EMPTY.equals(this.status) && this.occupiedBike == null;
    }
}
