package com.sharecycle.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sharecycle.domain.model.Bill;
import com.sharecycle.domain.model.Bike;
import com.sharecycle.domain.model.LedgerEntry;
import com.sharecycle.domain.model.Operator;
import com.sharecycle.domain.model.PricingPlan;
import com.sharecycle.domain.model.Reservation;
import com.sharecycle.domain.model.Rider;
import com.sharecycle.domain.model.Station;
import com.sharecycle.domain.model.Trip;
import com.sharecycle.domain.repository.JpaBikeRepository;
import com.sharecycle.domain.repository.JpaLedgerEntryRepository;
import com.sharecycle.domain.repository.JpaStationRepository;
import com.sharecycle.domain.repository.ReservationRepository;
import com.sharecycle.domain.repository.TripRepository;
import com.sharecycle.domain.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ResetSystemUseCaseTest {

    @Autowired
    private ResetSystemUseCase resetSystemUseCase;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JpaStationRepository stationRepository;
    @Autowired
    private JpaBikeRepository bikeRepository;
    @Autowired
    private TripRepository tripRepository;
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private JpaLedgerEntryRepository ledgerEntryRepository;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private ObjectMapper objectMapper;

    private Operator operator;
    private Rider rider;

    @BeforeEach
    void setUpUsers() {
        operator = new Operator("Reset Operator", "1 Admin Way", "reset@sharecycle.com", "reset-operator", "hash", null);
        rider = new Rider("Reset Rider", "2 Rider Way", "reset-rider@sharecycle.com", "reset-rider", "hash", "pm_token", PricingPlan.PlanType.PAY_AS_YOU_GO);
        userRepository.save(operator);
        userRepository.save(rider);
    }

    @Test
    void clearsMutableTablesAndReloadsSeedData() throws IOException {
        seedMutableData();

        ResetSystemUseCase.ResetSummary summary = resetSystemUseCase.execute(operator.getUserId());

        assertThat(countEntities("JpaLedgerEntryEntity")).isZero();
        assertThat(countEntities("JpaReservationEntity")).isZero();
        assertThat(countEntities("JpaTripEntity")).isZero();

        var bikes = bikeRepository.findAll();
        var stations = stationRepository.findAll();
        assertThat(bikes).hasSize(expectedBikeCount());
        assertThat(stations).hasSize(expectedStationCount());
        int dockCount = stations.stream().mapToInt(station -> station.getDocks().size()).sum();
        assertThat(summary.docks()).isEqualTo(dockCount);
        assertThat(summary.bikes()).isEqualTo(bikes.size());
        assertThat(summary.stations()).isEqualTo(stations.size());
    }

    private void seedMutableData() {
        Station station = new Station();
        station.setName("Temp Station");
        station.setLatitude(45.0);
        station.setLongitude(-73.0);
        station.setAddress("Somewhere");
        station.addEmptyDocks(1);

        Bike bike = new Bike();
        station.getDocks().get(0).setOccupiedBike(bike);

        stationRepository.save(station);
        bikeRepository.save(bike);

        Trip trip = new Trip(
                UUID.randomUUID(),
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now(),
                rider,
                bike,
                station,
                station
        );
        tripRepository.save(trip);

        Reservation reservation = new Reservation(
                UUID.randomUUID(),
                rider,
                station,
                bike,
                Instant.now(),
                Instant.now().plusSeconds(300),
                5,
                true
        );
        reservationRepository.save(reservation);

        Bill bill = new Bill(UUID.randomUUID(), LocalDateTime.now(), 1.0, 0.5, 0.0, 1.5);
        LedgerEntry ledgerEntry = new LedgerEntry(rider, trip, bill, "PAYG");
        ledgerEntryRepository.save(ledgerEntry);
    }

    private long countEntities(String entityName) {
        return entityManager.createQuery("select count(e) from " + entityName + " e", Long.class).getSingleResult();
    }

    private int expectedBikeCount() throws IOException {
        try (InputStream inputStream = new ClassPathResource("db/data/bikes.json").getInputStream()) {
            List<Map<String, Object>> bikes = objectMapper.readValue(inputStream, new TypeReference<>() {});
            return bikes.size();
        }
    }

    private int expectedStationCount() throws IOException {
        try (InputStream inputStream = new ClassPathResource("db/data/stations.json").getInputStream()) {
            List<Map<String, Object>> stations = objectMapper.readValue(inputStream, new TypeReference<>() {});
            return stations.size();
        }
    }
}

