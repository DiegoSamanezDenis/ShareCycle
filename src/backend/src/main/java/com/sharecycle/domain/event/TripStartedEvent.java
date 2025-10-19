package com.sharecycle.domain.event;

import com.sharecycle.domain.model.Rider;
import com.sharecycle.domain.model.Station;
import com.sharecycle.domain.model.Bike;


import java.time.LocalDateTime;
import java.util.UUID;

public record TripStartedEvent(
        UUID tripID,
        LocalDateTime startTime,
        LocalDateTime endTime,
        int durationMinutes,
        Rider rider,
        Bike bike,
        Station startStation,
        Station endStation
) {
}
