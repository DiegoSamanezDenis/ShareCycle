package com.sharecycle.application;

import com.sharecycle.domain.model.Bike;
import com.sharecycle.domain.model.Rider;
import com.sharecycle.domain.model.Station;
import com.sharecycle.domain.model.Reservation;
import com.sharecycle.domain.repository.JpaBikeRepository;
import com.sharecycle.domain.repository.JpaStationRepository;
import com.sharecycle.domain.repository.ReservationRepository;
import com.sharecycle.domain.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
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

    @Test
    @Transactional
    void reservesBikeAndPersistsBikeStatus() {
        // rider
        Rider rider = new Rider("Rider Name", "123 Street", "rider@example.com", "rider1", "hash", "tok_abc");
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
}


