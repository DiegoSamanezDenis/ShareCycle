package com.sharecycle.integration; // keep whatever package you have

import com.sharecycle.application.ReservationExpiryScheduler;
import com.sharecycle.application.ReserveBikeUseCase;
import com.sharecycle.application.StartTripUseCase;
import com.sharecycle.domain.model.*;
import com.sharecycle.domain.repository.*;
import com.sharecycle.infrastructure.persistence.JpaTripRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.*;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
public class ReservationFlowIntegrationTests {

    @Autowired
    private ReservationExpiryScheduler scheduler;

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
    private StartTripUseCase startTripUseCase;

    @Autowired
    private JpaTripRepository tripRepository;

    @Test
    @Transactional
    void reservationExpiredFlow() {
        // Create riders
        Rider rider_og = new Rider("Original Rider", "123 Street", "rider2@example.com", "rider2", "hash", "tok_xyz", PricingPlan.PlanType.PAY_AS_YOU_GO);
        Rider rider_new = new Rider("New Rider", "123 Street", "rider2@example.com", "rider3", "hash", "tok_xyz", PricingPlan.PlanType.PAY_AS_YOU_GO);
        userRepository.save(rider_og);
        userRepository.save(rider_new);

        // Create station with dock
        Station station = new Station();
        station.setName("Expiry Station");
        station.setLatitude(45.0);
        station.setLongitude(-73.0);
        station.setAddress("Address");
        station.markActive();
        station.addEmptyDocks(1);
        // Create Bike
        Bike bike = new Bike(Bike.BikeType.STANDARD);
        bike.setStatus(Bike.BikeStatus.AVAILABLE);
        station.getDocks().getFirst().setOccupiedBike(bike);
        stationRepository.save(station);

        // Reserve the bike
        Reservation reservation = reserveBikeUseCase.execute(rider_og, station, bike, 5);

        // Check reservation while still active
        scheduler.expireReservations();

        // verify reservation active and bike reserved in persistence
        Reservation loaded = reservationRepository.findById(reservation.getReservationId());
        assertThat(loaded.isMarkedActive()).isTrue();
        Bike persistedBike = bikeRepository.findById(bike.getId());
        assertThat(persistedBike.getStatus()).isEqualTo(Bike.BikeStatus.RESERVED);

        // While reservation is active, no other rider can start
        assertThrows(IllegalStateException.class, () -> startTripUseCase.execute(UUID.randomUUID(), LocalDateTime.now(), 0, rider_new, bike, station));

        // Offset time by 10 minutes into the future
        try (MockedStatic<Instant> mocked =
                     Mockito.mockStatic(Instant.class, Mockito.CALLS_REAL_METHODS)) {
            Clock baseClock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
            Clock offsetClock = Clock.offset(baseClock, Duration.ofMinutes(5));
            Instant futureTime = Instant.now(offsetClock);
            mocked.when(Instant::now).thenReturn(futureTime);

            // Run reservation with mocked time
            scheduler.expireReservations();
        }
        // Verify reservation is expired and bike is available
        loaded = reservationRepository.findById(reservation.getReservationId());
        assertThat(loaded.isMarkedActive()).isFalse();
        persistedBike = bikeRepository.findById(bike.getId());
        assertThat(persistedBike.getStatus()).isEqualTo(Bike.BikeStatus.AVAILABLE);

        // Check if anyone can start trip with that bike
        Trip trip = startTripUseCase.execute(UUID.randomUUID(), LocalDateTime.now(), 0, rider_new, bike, station);
        Trip managedTrip = tripRepository.findById(trip.getTripID());
        assertNotNull(managedTrip);
    }
}
