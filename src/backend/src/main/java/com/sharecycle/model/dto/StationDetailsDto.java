package com.sharecycle.model.dto;

import com.sharecycle.domain.model.Dock;
import com.sharecycle.domain.model.Station;

import java.util.List;
import java.util.UUID;

public record StationDetailsDto(
        UUID stationId,
        String name,
        Station.StationStatus status,
        int capacity,
        int bikesDocked,
        int freeDocks,
        List<DockDto> docks
) {
    public record DockDto(
            UUID dockId,
            Dock.DockStatus status,
            UUID bikeId
    ) { }
}
