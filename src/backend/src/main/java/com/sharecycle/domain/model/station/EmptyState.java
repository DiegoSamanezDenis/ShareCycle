package com.sharecycle.domain.model.station;

import com.sharecycle.domain.model.Station;

final class EmptyState implements StationState {

    static final EmptyState INSTANCE = new EmptyState();

    private EmptyState() {
    }

    @Override
    public Station.StationStatus getStatus() {
        return Station.StationStatus.EMPTY;
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


