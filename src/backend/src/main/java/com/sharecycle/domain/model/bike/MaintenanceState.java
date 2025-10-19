package com.sharecycle.domain.model.bike;

import com.sharecycle.domain.model.Bike;

public class MaintenanceState implements BikeState {

    public static final MaintenanceState INSTANCE = new MaintenanceState();

    private MaintenanceState() {
    }

    @Override
    public Bike.BikeStatus getStatus() {
        return Bike.BikeStatus.MAINTENANCE;
    }

    @Override
    public BikeState reserve(Bike bike) {
        throw new IllegalStateException("Bike under maintenance cannot be reserved.");
    }

    @Override
    public BikeState checkout(Bike bike) {
        throw new IllegalStateException("Bike under maintenance cannot be checked out.");
    }

    @Override
    public BikeState completeTrip(Bike bike) {
        throw new IllegalStateException("Bike under maintenance has no active trip.");
    }

    @Override
    public BikeState markAvailable(Bike bike) {
        return AvailableState.INSTANCE;
    }

    @Override
    public BikeState sendToMaintenance(Bike bike) {
        return this;
    }
}
