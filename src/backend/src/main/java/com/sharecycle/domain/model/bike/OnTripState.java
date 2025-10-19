package com.sharecycle.domain.model.bike;

import com.sharecycle.domain.model.Bike;

public class OnTripState implements BikeState {

    public static final OnTripState INSTANCE = new OnTripState();

    private OnTripState() {
    }

    @Override
    public Bike.BikeStatus getStatus() {
        return Bike.BikeStatus.ON_TRIP;
    }

    @Override
    public BikeState reserve(Bike bike) {
        throw new IllegalStateException("Bike is currently on a trip.");
    }

    @Override
    public BikeState checkout(Bike bike) {
        throw new IllegalStateException("Bike is already checked out.");
    }

    @Override
    public BikeState completeTrip(Bike bike) {
        return AvailableState.INSTANCE;
    }

    @Override
    public BikeState markAvailable(Bike bike) {
        return AvailableState.INSTANCE;
    }

    @Override
    public BikeState sendToMaintenance(Bike bike) {
        return MaintenanceState.INSTANCE;
    }
}
