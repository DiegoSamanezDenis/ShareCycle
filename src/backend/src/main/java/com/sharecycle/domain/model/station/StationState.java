package com.sharecycle.domain.model.station;

import com.sharecycle.domain.model.Station;

public interface StationState {

    Station.StationStatus getStatus();

    StationState onCountsUpdated(Station station);

    StationState markOutOfService(Station station);

    StationState markActive(Station station);
}


