package com.sharecycle.domain.model.station;

import com.sharecycle.domain.model.Station;

final class StationStateSupport {

    private StationStateSupport() {
    }

    static StationState determineByCapacity(Station station) {
        int bikesDocked = station.getBikesDocked();
        int capacity = station.getCapacity();

        if (bikesDocked <= 0) {
            return EmptyState.INSTANCE;
        }

        if (capacity <= 0) {
            return bikesDocked > 0 ? FullState.INSTANCE : EmptyState.INSTANCE;
        }

        if (bikesDocked >= capacity) {
            return FullState.INSTANCE;
        }

        return OccupiedState.INSTANCE;
    }
}


