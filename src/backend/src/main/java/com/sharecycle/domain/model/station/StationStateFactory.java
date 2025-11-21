package com.sharecycle.domain.model.station;

import com.sharecycle.domain.model.Station;

public final class StationStateFactory {

    private StationStateFactory() {
    }

    public static StationState fromStatus(Station.StationStatus status) {
        Station.StationStatus safeStatus = status == null ? Station.StationStatus.EMPTY : status;
        return switch (safeStatus) {
            case EMPTY -> EmptyState.INSTANCE;
            case OCCUPIED -> OccupiedState.INSTANCE;
            case FULL -> FullState.INSTANCE;
            case OUT_OF_SERVICE -> OutOfServiceState.INSTANCE;
        };
    }

    public static StationState fromCounts(Station station) {
        return StationStateSupport.determineByCapacity(station);
    }
}


