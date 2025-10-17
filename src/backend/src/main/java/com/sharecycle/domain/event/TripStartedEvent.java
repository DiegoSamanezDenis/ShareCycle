package com.sharecycle.domain.event;

import com.sharecycle.model.entity.Rider;
import com.sharecycle.model.entity.Station;
import com.sharecycle.model.entity.Bike;


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
