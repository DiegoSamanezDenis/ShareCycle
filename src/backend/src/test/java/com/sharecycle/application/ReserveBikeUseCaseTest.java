package com.sharecycle.application;

import com.sharecycle.domain.model.Bike;
import com.sharecycle.domain.model.PricingPlan;
import com.sharecycle.domain.model.Reservation;
import com.sharecycle.domain.model.Rider;
import com.sharecycle.domain.model.Station;
import com.sharecycle.domain.model.Trip;
import com.sharecycle.domain.repository.JpaBikeRepository;
import com.sharecycle.domain.repository.JpaStationRepository;
import com.sharecycle.domain.repository.ReservationRepository;
import com.sharecycle.domain.repository.TripRepository;
import com.sharecycle.domain.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Import(PaymentGatewayTestConfig.class)
class ReserveBikeUseCaseTest {

    @Autowired
    private ReserveBikeUseCase reserveBikeUseCase;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private JpaBikeRepository bikeRepository;

    @Autowired
    private JpaStationRepository stationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TripRepository tripRepository;

    @Test
    @Transactional
    void reservesBikeAndPersistsBikeStatus() {
        // rider
        Rider rider = new Rider("Rider Name", "123 Street", "rider@example.com", "rider1", "hash", "tok_abc", PricingPlan.PlanType.PAY_AS_YOU_GO);
        userRepository.save(rider);

        // station with an available bike
        Station station = new Station();
        station.setName("Reserve Station");
        station.setLatitude(45.0);
        station.setLongitude(-73.0);
        station.setAddress("Address");
        station.markActive();
        station.addEmptyDocks(1);
        Bike bike = new Bike(Bike.BikeType.STANDARD);
        station.getDocks().getFirst().setOccupiedBike(bike);
        stationRepository.save(station);

        // reserve
        Reservation reservation = reserveBikeUseCase.execute(rider, station, bike, 5);

        // assert reservation persisted
        Reservation loaded = reservationRepository.findById(reservation.getReservationId());
        assertThat(loaded).isNotNull();
        assertThat(loaded.isActive()).isTrue();
        assertThat(loaded.getRider().getUserId()).isEqualTo(rider.getUserId());

        // assert bike status persisted as RESERVED
        Bike persistedBike = bikeRepository.findById(bike.getId());
        assertThat(persistedBike.getStatus()).isEqualTo(Bike.BikeStatus.RESERVED);
    }

    @Test
    @Transactional
    void failsWhenRiderHasActiveTrip() {
        Rider rider = new Rider("Trip Rider", "456 Street", "triprider@example.com", "riderTrip", "hash", "tok_trip", PricingPlan.PlanType.PAY_AS_YOU_GO);
        userRepository.save(rider);

        Station reservationStation = new Station();
        reservationStation.setName("Reservation Station");
        reservationStation.setLatitude(45.5);
        reservationStation.setLongitude(-73.5);
        reservationStation.setAddress("Reservation Address");
        reservationStation.markActive();
        reservationStation.addEmptyDocks(1);
        Bike reservationBike = new Bike(Bike.BikeType.STANDARD);
        reservationStation.getDocks().getFirst().setOccupiedBike(reservationBike);
        stationRepository.save(reservationStation);

        Station tripStartStation = new Station();
        tripStartStation.setName("Trip Start Station");
        tripStartStation.setLatitude(45.6);
        tripStartStation.setLongitude(-73.6);
        tripStartStation.setAddress("Trip Start Address");
        tripStartStation.markActive();
        tripStartStation.addEmptyDocks(1);
        stationRepository.save(tripStartStation);

        Bike tripBike = new Bike(Bike.BikeType.STANDARD);
        tripBike.setStatus(Bike.BikeStatus.ON_TRIP);
        bikeRepository.save(tripBike);

        Trip activeTrip = new Trip(
                UUID.randomUUID(),
                LocalDateTime.now(),
                null,
                rider,
                tripBike,
                tripStartStation,
                null
        );
        tripRepository.save(activeTrip);

        assertThatThrownBy(() -> reserveBikeUseCase.execute(rider, reservationStation, reservationBike, 5))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("active trip");
    }
}


