package com.sharecycle.infrastructure.persistence.jpa;

import com.sharecycle.domain.model.Dock;
import com.sharecycle.domain.model.Station;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "station")
public class JpaStationEntity {

    @Id
    @Column(name = "station_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID stationId;

    @Column(name = "station_name")
    private String name;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "station_status", nullable = false)
    private Station.StationStatus status;

    @Column(name = "station_latitude", nullable = false)
    private double latitude;

    @Column(name = "station_longtitude", nullable = false)
    private double longitude;

    @Column(name = "bikes_docked", nullable = false)
    private int bikesDocked;

    @Column(name = "station_capacity", nullable = false)
    private int capacity;

    @Column(name = "address", nullable = false)
    private String address;

    @OneToMany(mappedBy = "station", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<JpaDockEntity> docks = new ArrayList<>();

    public JpaStationEntity() {
    }

    private JpaStationEntity(Station station) {
        this.stationId = station.getId();
        this.name = station.getName();
        this.status = station.getStatus();
        this.latitude = station.getLatitude();
        this.longitude = station.getLongitude();
        this.bikesDocked = station.getBikesDocked();
        this.capacity = station.getCapacity();
        this.address = station.getAddress();
    }

    public static JpaStationEntity fromDomain(Station station, MapperContext context) {
        JpaStationEntity existing = context.stationEntities.get(station.getId());
        if (existing != null) {
            return existing;
        }
        JpaStationEntity entity = new JpaStationEntity(station);
        context.stationEntities.put(station.getId(), entity);
        context.stations.put(station.getId(), station);
        entity.docks = station.getDocks().stream()
                .map(dock -> {
                    JpaDockEntity jpaDock = JpaDockEntity.fromDomain(dock, context);
                    jpaDock.setStation(entity);
                    return jpaDock;
                })
                .collect(Collectors.toCollection(ArrayList::new));
        return entity;
    }

    public Station toDomain(MapperContext context) {
        Station existing = context.stations.get(stationId);
        if (existing != null) {
            return existing;
        }
        Station station = new Station(stationId, name, status, latitude, longitude, address, capacity, bikesDocked);
        context.stations.put(stationId, station);
        List<Dock> domainDocks = docks.stream()
                .map(dock -> dock.toDomain(context))
                .collect(Collectors.toCollection(ArrayList::new));
        station.setDocks(domainDocks);
        return station;
    }

    public UUID getStationId() {
        return stationId;
    }

    public void setStationId(UUID stationId) {
        this.stationId = stationId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Station.StationStatus getStatus() {
        return status;
    }

    public void setStatus(Station.StationStatus status) {
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

    public List<JpaDockEntity> getDocks() {
        return docks;
    }

    public void setDocks(List<JpaDockEntity> docks) {
        this.docks = docks;
    }
}
