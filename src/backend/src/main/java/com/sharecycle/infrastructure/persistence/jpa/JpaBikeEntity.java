package com.sharecycle.infrastructure.persistence.jpa;

import com.sharecycle.domain.model.Bike;
import com.sharecycle.domain.model.Station;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bike")
public class JpaBikeEntity {

    @Id
    @Column(name = "bike_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID bikeId;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "bike_type", nullable = false)
    private Bike.BikeType type;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "bike_status", nullable = false)
    private Bike.BikeStatus status;

    @Column(name = "reservation_expiry")
    private Instant reservationExpiry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_station_id")
    private JpaStationEntity currentStation;

    public JpaBikeEntity() {
    }

    private JpaBikeEntity(Bike bike) {
        this.bikeId = bike.getId();
        this.type = bike.getType();
        this.status = bike.getStatus();
        this.reservationExpiry = bike.getReservationExpiry();
    }

    public static JpaBikeEntity fromDomain(Bike bike, MapperContext context) {
        JpaBikeEntity existing = context.bikeEntities.get(bike.getId());
        if (existing != null) {
            return existing;
        }
        JpaBikeEntity entity = new JpaBikeEntity(bike);
        context.bikeEntities.put(bike.getId(), entity);
        context.bikes.put(bike.getId(), bike);
        Station currentStation = bike.getCurrentStation();
        if (currentStation != null) {
            entity.currentStation = JpaStationEntity.fromDomain(currentStation, context);
        }
        return entity;
    }

    public Bike toDomain(MapperContext context) {
        Bike existing = context.bikes.get(bikeId);
        if (existing != null) {
            return existing;
        }
        Bike bike = new Bike(bikeId, type, status, reservationExpiry, null);
        context.bikes.put(bikeId, bike);
        if (currentStation != null) {
            Station station = currentStation.toDomain(context);
            bike.setCurrentStation(station);
        }
        return bike;
    }

    public UUID getBikeId() {
        return bikeId;
    }

    public void setBikeId(UUID bikeId) {
        this.bikeId = bikeId;
    }

    public Bike.BikeType getType() {
        return type;
    }

    public void setType(Bike.BikeType type) {
        this.type = type;
    }

    public Bike.BikeStatus getStatus() {
        return status;
    }

    public void setStatus(Bike.BikeStatus status) {
        this.status = status;
    }

    public Instant getReservationExpiry() {
        return reservationExpiry;
    }

    public void setReservationExpiry(Instant reservationExpiry) {
        this.reservationExpiry = reservationExpiry;
    }

    public JpaStationEntity getCurrentStation() {
        return currentStation;
    }

    public void setCurrentStation(JpaStationEntity currentStation) {
        this.currentStation = currentStation;
    }
}
