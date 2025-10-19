package com.sharecycle.domain.model.bike;

import com.sharecycle.domain.model.Bike;

public class ReservedState implements BikeState {

    public static final ReservedState INSTANCE = new ReservedState();

    private ReservedState() {
    }

    @Override
    public Bike.BikeStatus getStatus() {
        return Bike.BikeStatus.RESERVED;
    }

    @Override
    public BikeState reserve(Bike bike) {
        throw new IllegalStateException("Bike is already reserved.");
    }

    @Override
    public BikeState checkout(Bike bike) {
        return OnTripState.INSTANCE;
    }

    @Override
    public BikeState completeTrip(Bike bike) {
        throw new IllegalStateException("Cannot complete trip from reserved state.");
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
