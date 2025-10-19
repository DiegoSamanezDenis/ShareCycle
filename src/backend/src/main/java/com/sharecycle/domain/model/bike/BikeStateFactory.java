package com.sharecycle.domain.model.bike;

import com.sharecycle.domain.model.Bike;

public final class BikeStateFactory {

    private BikeStateFactory() {
    }

    public static BikeState fromStatus(Bike.BikeStatus status) {
        return switch (status) {
            case AVAILABLE -> AvailableState.INSTANCE;
            case RESERVED -> ReservedState.INSTANCE;
            case ON_TRIP -> OnTripState.INSTANCE;
            case MAINTENANCE -> MaintenanceState.INSTANCE;
        };
    }
}
