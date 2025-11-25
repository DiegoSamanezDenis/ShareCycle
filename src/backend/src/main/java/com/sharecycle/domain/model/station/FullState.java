package com.sharecycle.domain.model.station;

import com.sharecycle.domain.model.Station;

final class FullState implements StationState {

    static final FullState INSTANCE = new FullState();

    private FullState() {
    }

    @Override
    public Station.StationStatus getStatus() {
        return Station.StationStatus.FULL;
    }

    @Override
    public StationState onCountsUpdated(Station station) {
        return StationStateSupport.determineByCapacity(station);
    }

    @Override
    public StationState markOutOfService(Station station) {
        return OutOfServiceState.INSTANCE;
    }

    @Override
    public StationState markActive(Station station) {
        return StationStateSupport.determineByCapacity(station);
    }
}


