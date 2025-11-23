package com.sharecycle.application;

import com.sharecycle.domain.model.*;
import com.sharecycle.domain.repository.JpaBikeRepository;
import com.sharecycle.domain.repository.JpaLedgerEntryRepository;
import com.sharecycle.infrastructure.persistence.JpaTripRepository;
import com.sharecycle.infrastructure.persistence.JpaUserRepository;
import com.sharecycle.service.payment.PaymentGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(classes = {ListTripsUseCaseTest.PaymentGatewayTestConfig.class})
class ListTripsUseCaseTest {

    @Autowired
    private ListTripsUseCase listTripsUseCase;

    @Autowired
    private JpaLedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private JpaUserRepository userRepository;

    @Autowired
    private JpaTripRepository tripRepository;

    @Autowired
    private JpaBikeRepository bikeRepository;

    private Rider testUser;
    private Trip testTrip;
    private Bike testBike;

    @BeforeEach
    void setup() {
        createUser();
        createBike();
        createTrip();
        createLedgerEntry();
    }

    @Test
    @Transactional
    void correctTripSearch() {
        ListTripsUseCase.TripHistoryPage returnedTrip = listTripsUseCase.execute(
                testUser,
                LocalDateTime.of(2000, 1, 1, 0, 0),
                LocalDateTime.of(2100, 1, 1, 0, 0),
                Bike.BikeType.E_BIKE,
                0,
                8,
                null,
                "RIDER"
        );

        assertEquals(1, returnedTrip.entries().size());
        assertEquals(1, returnedTrip.totalItems());
        assertEquals(1, returnedTrip.totalPages());
        var entry = returnedTrip.entries().get(0);
        assertEquals(testTrip.getTripID(), entry.tripId());
        assertEquals(testUser.getUserId(), entry.riderId());
        assertEquals("Test", entry.riderName());
        assertEquals(10.50, entry.totalCost(), 0.0001);
    }

    @Test
    @Transactional
    void incorrectTripSearch() {
        ListTripsUseCase.TripHistoryPage returnedTrip = listTripsUseCase.execute(
                testUser,
                LocalDateTime.of(1999, 1, 1, 0, 0),
                LocalDateTime.of(1999, 2, 1, 0, 0),
                Bike.BikeType.E_BIKE,
                0,
                8,
                null,
                "RIDER"
        );
        assertTrue(returnedTrip.entries().isEmpty());
        assertEquals(0, returnedTrip.totalItems());
        assertEquals(0, returnedTrip.totalPages());
    }

    @Test
    @Transactional
    void filtersByTripIdSubstring() {
        String partialId = testTrip.getTripID().toString().substring(0, 8);
        ListTripsUseCase.TripHistoryPage returnedTrip = listTripsUseCase.execute(
                testUser,
                null,
                null,
                null,
                0,
                8,
                partialId,
                "RIDER"
        );
        assertEquals(1, returnedTrip.totalItems());
        assertEquals(1, returnedTrip.entries().size());
        assertEquals(testTrip.getTripID(), returnedTrip.entries().get(0).tripId());
    }

    private void createUser() {
        testUser = new Rider();
        testUser.setUserId(UUID.randomUUID());
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
        testUser.setEmail("test@test.com");
        testUser.setRole("RIDER");
        testUser.setFullName("Test");
        testUser.setPasswordHash("password");
        testUser.setStreetAddress("");
        testUser.setPaymentMethodToken(null);
        testUser.setUsername("Test");
        userRepository.save(testUser);
    }

    private void createBike() {
        testBike = new Bike();
        testBike.setId(UUID.randomUUID());
        testBike.setStatus(Bike.BikeStatus.AVAILABLE);
        testBike.setType(Bike.BikeType.E_BIKE);
        bikeRepository.save(testBike);
    }

    private void createTrip() {
        testTrip = new Trip(
                UUID.randomUUID(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                testUser,
                testBike,
                stationWithName("Start Station"),
                stationWithName("End Station")
        );
        tripRepository.save(testTrip);
    }

    private void createLedgerEntry() {
        Bill bill = new Bill(5.00, 3.50, 2.00); // total 10.50
        LedgerEntry ledgerEntry = new LedgerEntry(testUser, testTrip, bill, "PAYG");
        ledgerEntryRepository.save(ledgerEntry);
    }

    private Station stationWithName(String name) {
        Station station = new Station();
        station.setName(name);
        return station;
    }

    /**
     * Test PaymentGateway configuration
     */
    @TestConfiguration
    static class PaymentGatewayTestConfig {
        @Bean
        @Primary
        public PaymentGateway paymentGateway() {
            return new PaymentGateway() {
                @Override
                public boolean capture(double amount, String riderToken) {
                    return true;
                }

                @Override
                public String createPaymentToken(User user) {
                    return "dummy-token";
                }
            };
        }
    }
}
