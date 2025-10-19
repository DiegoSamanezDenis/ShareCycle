package com.sharecycle.domain.model.bike;

import com.sharecycle.domain.model.Bike;

public class AvailableState implements BikeState {

    public static final AvailableState INSTANCE = new AvailableState();

    private AvailableState() {
    }

    @Override
    public Bike.BikeStatus getStatus() {
        return Bike.BikeStatus.AVAILABLE;
    }

    @Override
    public BikeState reserve(Bike bike) {
        return ReservedState.INSTANCE;
    }

    @Override
    public BikeState checkout(Bike bike) {
        return OnTripState.INSTANCE;
    }

    @Override
    public BikeState completeTrip(Bike bike) {
        return this;
    }

    @Override
    public BikeState markAvailable(Bike bike) {
        return this;
    }

    @Override
    public BikeState sendToMaintenance(Bike bike) {
        return MaintenanceState.INSTANCE;
    }
}
