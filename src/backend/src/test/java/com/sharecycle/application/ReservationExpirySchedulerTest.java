package com.sharecycle.application;

import com.sharecycle.domain.model.Bike;
import com.sharecycle.domain.model.PricingPlan;
import com.sharecycle.domain.model.Reservation;
import com.sharecycle.domain.model.Rider;
import com.sharecycle.domain.model.Station;
import com.sharecycle.domain.repository.JpaBikeRepository;
import com.sharecycle.domain.repository.JpaStationRepository;
import com.sharecycle.domain.repository.ReservationRepository;
import com.sharecycle.domain.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ReservationExpirySchedulerTest {

    @Autowired
    private ReservationExpiryScheduler scheduler;

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
    void expiresReservationAndPersistsBikeAvailable() {
        // rider
        Rider rider = new Rider("Rider Name", "123 Street", "rider2@example.com", "rider2", "hash", "tok_xyz", PricingPlan.PlanType.PAY_AS_YOU_GO);
        userRepository.save(rider);

        // station + reserved bike with expired timestamp
        Station station = new Station();
        station.setName("Expiry Station");
        station.setLatitude(45.0);
        station.setLongitude(-73.0);
        station.setAddress("Address");
        station.markActive();
        station.addEmptyDocks(1);
        Bike bike = new Bike(Bike.BikeType.STANDARD);
        bike.setStatus(Bike.BikeStatus.RESERVED);
        station.getDocks().getFirst().setOccupiedBike(bike);
        stationRepository.save(station);

        // create expired reservation manually
        Reservation reservation = new Reservation(
                null,
                rider,
                station,
                bike,
                Instant.now().minusSeconds(600),
                Instant.now().minusSeconds(60),
                5,
                true
        );
        reservationRepository.save(reservation);

        // run scheduler
        scheduler.expireReservations();

        // verify reservation inactive and bike available in persistence
        Reservation loaded = reservationRepository.findById(reservation.getReservationId());
        assertThat(loaded.isMarkedActive()).isFalse();
        Bike persistedBike = bikeRepository.findById(bike.getId());
        assertThat(persistedBike.getStatus()).isEqualTo(Bike.BikeStatus.AVAILABLE);
    }
}


