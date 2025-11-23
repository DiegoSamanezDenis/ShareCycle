package com.sharecycle.domain.model.station;

import com.sharecycle.domain.model.Station;

final class OutOfServiceState implements StationState {

    static final OutOfServiceState INSTANCE = new OutOfServiceState();

    private OutOfServiceState() {
    }

    @Override
    public Station.StationStatus getStatus() {
        return Station.StationStatus.OUT_OF_SERVICE;
    }

    @Override
    public StationState onCountsUpdated(Station station) {
        return this;
    }

    @Override
    public StationState markOutOfService(Station station) {
        return this;
    }

    @Override
    public StationState markActive(Station station) {
        return StationStateSupport.determineByCapacity(station);
    }
}


