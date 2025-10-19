package com.sharecycle.infrastructure.persistence.jpa;

import com.sharecycle.domain.model.Dock;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "dock")
public class JpaDockEntity {

    @Id
    @Column(name = "dock_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID dockId;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "dock_status", nullable = false)
    private Dock.DockStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id")
    private JpaStationEntity station;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "bike_id")
    private JpaBikeEntity occupiedBike;

    public JpaDockEntity() {
    }

    private JpaDockEntity(Dock dock) {
        this.dockId = dock.getId();
        this.status = dock.getStatus();
    }

    public static JpaDockEntity fromDomain(Dock dock, MapperContext context) {
        JpaDockEntity entity = new JpaDockEntity(dock);
        if (dock.getOccupiedBike() != null) {
            entity.occupiedBike = JpaBikeEntity.fromDomain(dock.getOccupiedBike(), context);
        }
        return entity;
    }

    public Dock toDomain(MapperContext context) {
        Dock dock = new Dock(dockId, status, null);
        if (occupiedBike != null) {
            dock.setOccupiedBike(occupiedBike.toDomain(context));
        }
        return dock;
    }

    public UUID getDockId() {
        return dockId;
    }

    public void setDockId(UUID dockId) {
        this.dockId = dockId;
    }

    public Dock.DockStatus getStatus() {
        return status;
    }

    public void setStatus(Dock.DockStatus status) {
        this.status = status;
    }

    public JpaStationEntity getStation() {
        return station;
    }

    public void setStation(JpaStationEntity station) {
        this.station = station;
    }

    public JpaBikeEntity getOccupiedBike() {
        return occupiedBike;
    }

    public void setOccupiedBike(JpaBikeEntity occupiedBike) {
        this.occupiedBike = occupiedBike;
    }
}
