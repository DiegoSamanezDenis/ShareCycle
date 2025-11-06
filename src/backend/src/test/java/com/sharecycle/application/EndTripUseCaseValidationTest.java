package com.sharecycle.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sharecycle.domain.TripBuilder;
import com.sharecycle.domain.event.DomainEventPublisher;
import com.sharecycle.domain.model.Bike;
import com.sharecycle.domain.model.Rider;
import com.sharecycle.domain.model.Station;
import com.sharecycle.domain.model.Trip;
import com.sharecycle.domain.repository.JpaBikeRepository;
import com.sharecycle.domain.repository.JpaLedgerEntryRepository;
import com.sharecycle.domain.repository.JpaStationRepository;
import com.sharecycle.domain.repository.ReservationRepository;
import com.sharecycle.domain.repository.TripRepository;

@ExtendWith(MockitoExtension.class)
class EndTripAndBillUseCaseValidationTest {

    @Mock
    private DomainEventPublisher eventPublisher;
    @Mock
    private TripRepository tripRepository;
    @Mock
    private JpaLedgerEntryRepository ledgerEntryRepository;
    @Mock
    private JpaStationRepository stationRepository;
    @Mock
    private JpaBikeRepository bikeRepository;
    @Mock
    private ReservationRepository reservationRepository;

    private EndTripAndBillUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new EndTripAndBillUseCase(
                eventPublisher,
                tripRepository,
                ledgerEntryRepository,
                stationRepository,
                bikeRepository,
                reservationRepository
        );
    }

    @Test
    void rejectsAlreadyCompletedTrip() {
        Trip endedTrip = buildTrip();
        TripBuilder builder = new TripBuilder(endedTrip);
        builder.endAt(buildStation(UUID.randomUUID()), LocalDateTime.now());
        Trip completed = builder.build();

        when(tripRepository.findById(completed.getTripID())).thenReturn(completed);

        assertThatThrownBy(() -> useCase.execute(completed, buildStation(UUID.randomUUID())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already completed");
    }

    @Test
    void rejectsOutOfServiceDestination() {
        Trip activeTrip = buildTrip();
        Station destination = buildStation(UUID.randomUUID());
        destination.markOutOfService();

        when(tripRepository.findById(activeTrip.getTripID())).thenReturn(activeTrip);
        when(stationRepository.findByIdForUpdate(destination.getId())).thenReturn(destination);

        assertThatThrownBy(() -> useCase.execute(activeTrip, destination))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("out of service");

        verify(ledgerEntryRepository, never()).save(any());
    }

    private Trip buildTrip() {
        Rider rider = new Rider();
        rider.setUserId(UUID.randomUUID());
        rider.setRole("RIDER");

        Station startStation = buildStation(UUID.randomUUID());
        Bike bike = new Bike();
        bike.setId(UUID.randomUUID());
        bike.setStatus(Bike.BikeStatus.ON_TRIP);

        TripBuilder builder = new TripBuilder();
        builder.setTripId(UUID.randomUUID());
        builder.start(rider, startStation, bike, LocalDateTime.now());
        return builder.build();
    }

    private Station buildStation(UUID id) {
        Station station = new Station();
        station.setId(id);
        station.markActive();
        station.addEmptyDocks(1);
        return station;
    }
}
