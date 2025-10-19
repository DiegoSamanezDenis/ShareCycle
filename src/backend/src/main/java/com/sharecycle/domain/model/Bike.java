package com.sharecycle.domain.model;

import java.time.Instant;
import java.util.UUID;

import com.sharecycle.domain.model.bike.BikeState;
import com.sharecycle.domain.model.bike.BikeStateFactory;


public class Bike {

    public enum BikeStatus {
        AVAILABLE, RESERVED, ON_TRIP, MAINTENANCE
    }

    public enum BikeType {
        STANDARD, E_BIKE
    }

    private UUID id;
    private BikeType type;
    private BikeStatus status;
    private BikeState state;
    private Instant reservationExpiry;
    private Station currentStation;

    public Bike() {
        this(UUID.randomUUID(), BikeType.STANDARD, BikeStatus.AVAILABLE, null, null);
    }

    public Bike(BikeType type) {
        this(UUID.randomUUID(), type, BikeStatus.AVAILABLE, null, null);
    }

    public Bike(UUID id,
                BikeType type,
                BikeStatus status,
                Instant reservationExpiry,
                Station currentStation) {
        this.id = id == null ? UUID.randomUUID() : id;
        this.type = type;
        applyState(BikeStateFactory.fromStatus(status));
        this.reservationExpiry = reservationExpiry;
        this.currentStation = currentStation;
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
        applyState(BikeStateFactory.fromStatus(status));
    }

    public Instant getReservationExpiry() {
        return reservationExpiry;
    }

    public void setReservationExpiry(Instant reservationExpiry) {
        this.reservationExpiry = reservationExpiry;
    }

    public Station getCurrentStation() {
        return currentStation;
    }

    public void setCurrentStation(Station currentStation) {
        this.currentStation = currentStation;
    }

    public void reserve() {
        applyState(state.reserve(this));
    }

    public void checkout() {
        applyState(state.checkout(this));
    }

    public void completeTrip() {
        applyState(state.completeTrip(this));
    }

    public void markAvailable() {
        applyState(state.markAvailable(this));
    }

    public void sendToMaintenance() {
        applyState(state.sendToMaintenance(this));
    }

    private void applyState(BikeState newState) {
        this.state = newState;
        this.status = newState.getStatus();
    }
}
