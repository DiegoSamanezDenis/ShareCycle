package com.sharecycle.model.entity;


import jakarta.persistence.*;

import java.sql.Timestamp;
import java.util.UUID;

@Entity
public class Bike {

    public enum BikeStatus{
        AVAILABLE, RESERVED, ON_TRIP, MAINTENANCE
    }

    public enum BikeType{
        STANDARD, E_BIKE,
    }

    @Id
    @Column(name = "bike_id", columnDefinition = "BINARY(16)", unique = true, nullable = false)
    private UUID id;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "bike_type", nullable = false)
    private BikeType type;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "bike_status", nullable = false)
    private BikeStatus status;

    @Column(name = "reservation_expiry")
    private Timestamp reservationExpiry; //SQL Timestamp

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_station_id")
    private Station currentStation;

    public Bike() {
        this.id = UUID.randomUUID();
        this.type = BikeType.STANDARD;
        this.status = BikeStatus.AVAILABLE;
        this.reservationExpiry = null;
    }

    public Bike(BikeType type) {
        this.id = UUID.randomUUID();
        this.type = type;
        this.status = BikeStatus.AVAILABLE;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public BikeType getType() {
        return type;
    }

    public void setType(BikeType type) {
        this.type = type;
    }

    public BikeStatus getStatus() {
        return status;
    }

    public void setStatus(BikeStatus status) {
        this.status = status;
    }

    public Timestamp getReservationExpiry() {
        return reservationExpiry;
    }

    public void setReservationExpiry(Timestamp reservationExpiry) {
        this.reservationExpiry = reservationExpiry;
    }

    public Station getCurrentStation() {
        return currentStation;
    }

    public void setCurrentStation(Station currentStation) {
        this.currentStation = currentStation;
    }
}
