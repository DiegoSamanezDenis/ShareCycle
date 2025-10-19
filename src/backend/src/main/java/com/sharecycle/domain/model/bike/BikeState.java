package com.sharecycle.domain.model.bike;

import com.sharecycle.domain.model.Bike;

public interface BikeState {
    Bike.BikeStatus getStatus();

    BikeState reserve(Bike bike);

    BikeState checkout(Bike bike);

    BikeState completeTrip(Bike bike);

    BikeState markAvailable(Bike bike);

    BikeState sendToMaintenance(Bike bike);
}
