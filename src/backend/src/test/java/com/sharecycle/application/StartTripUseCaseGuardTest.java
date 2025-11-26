package com.sharecycle.application;

import com.sharecycle.domain.event.DomainEventPublisher;
import com.sharecycle.domain.model.Bike;
import com.sharecycle.domain.model.Rider;
import com.sharecycle.domain.model.Station;
import com.sharecycle.domain.repository.JpaBikeRepository;
import com.sharecycle.domain.repository.JpaStationRepository;
import com.sharecycle.domain.repository.ReservationRepository;
import com.sharecycle.domain.repository.TripRepository;
import com.sharecycle.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StartTripUseCaseGuardTest {

    @Mock
    private JpaBikeRepository bikeRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JpaStationRepository stationRepository;
    @Mock
    private TripRepository tripRepository;
    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private DomainEventPublisher eventPublisher;

    private StartTripUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new StartTripUseCase(
                bikeRepository,
                userRepository,
                stationRepository,
                tripRepository,
                reservationRepository,
                eventPublisher
        );
    }

    @Test
    void rejectsWhenRiderAlreadyHasActiveTrip() {
        Rider rider = buildRider();
        Bike bike = buildBike();
        Station station = buildStation(bike);

        lenient().when(userRepository.findById(rider.getUserId())).thenReturn(rider);
        lenient().when(tripRepository.riderHasActiveTrip(rider.getUserId())).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(
                UUID.randomUUID(),
                LocalDateTime.now(),
                0,
                rider,
                bike,
                station
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("active trip");

        verify(tripRepository, never()).save(any());
    }

    @Test
    void rejectsWhenBikeReservedByAnotherRider() {
        Rider rider = buildRider();
        Bike bike = buildBike();
        bike.setStatus(Bike.BikeStatus.RESERVED);
        Station station = buildStation(bike);

        lenient().when(userRepository.findById(rider.getUserId())).thenReturn(rider);
        lenient().when(tripRepository.riderHasActiveTrip(rider.getUserId())).thenReturn(false);
        lenient().when(bikeRepository.findById(bike.getId())).thenReturn(bike);
        lenient().when(stationRepository.findByIdForUpdate(station.getId())).thenReturn(station);
        lenient().when(reservationRepository.findByRiderId(rider.getUserId())).thenReturn(null);

        assertThatThrownBy(() -> useCase.execute(
                UUID.randomUUID(),
                LocalDateTime.now(),
                0,
                rider,
                bike,
                station
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("reserved by another rider");

        verify(tripRepository, never()).save(any());
    }

    private Rider buildRider() {
        Rider rider = new Rider();
        rider.setUserId(UUID.randomUUID());
        rider.setRole("RIDER");
        return rider;
    }

    private Bike buildBike() {
        Bike bike = new Bike();
        bike.setId(UUID.randomUUID());
        bike.setStatus(Bike.BikeStatus.AVAILABLE);
        return bike;
    }

    private Station buildStation(Bike bike) {
        Station station = new Station();
        station.setId(UUID.randomUUID());
        station.setName("Test Station");
        station.markActive();
        station.addEmptyDocks(1);
        station.getDocks().getFirst().setOccupiedBike(bike);
        return station;
    }
}