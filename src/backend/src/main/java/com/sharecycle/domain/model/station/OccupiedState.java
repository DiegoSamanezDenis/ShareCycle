package com.sharecycle.domain.model.station;

import com.sharecycle.domain.model.Station;

final class OccupiedState implements StationState {

    static final OccupiedState INSTANCE = new OccupiedState();

    private OccupiedState() {
    }

    @Override
    public Station.StationStatus getStatus() {
        return Station.StationStatus.OCCUPIED;
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


