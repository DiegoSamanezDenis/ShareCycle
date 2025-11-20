package com.sharecycle.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class TripModelTest {

    @Test
    void endTrip_setsEndStationAndRecalculatesDuration() {
        LocalDateTime start = LocalDateTime.now().minusMinutes(30);
        LocalDateTime end = LocalDateTime.now();

        Rider rider = new Rider();
        Bike bike = new Bike(Bike.BikeType.STANDARD);
        Station startStation = new Station();
        Station endStation = new Station();

        Trip trip = new Trip(UUID.randomUUID(), start, null, rider, bike, startStation, null);
        assertThat(trip.getDurationMinutes()).isEqualTo(0);

        trip.endTrip(endStation, end);

        assertThat(trip.getEndStation()).isSameAs(endStation);
        assertThat(trip.getEndTime()).isEqualTo(end);
        assertThat(trip.getDurationMinutes()).isGreaterThanOrEqualTo(29); // ~30
    }

    @Test
    void trip_withNullEnd_timeResultsInZeroDurationUntilEnded() {
        LocalDateTime start = LocalDateTime.now();
        Rider rider = new Rider();
        Bike bike = new Bike(Bike.BikeType.STANDARD);
        Station startStation = new Station();

        Trip trip = new Trip(UUID.randomUUID(), start, null, rider, bike, startStation, null);
        assertThat(trip.getDurationMinutes()).isEqualTo(0);
    }
}
