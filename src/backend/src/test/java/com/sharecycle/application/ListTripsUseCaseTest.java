package com.sharecycle.application;

import com.sharecycle.domain.model.*;
import com.sharecycle.domain.repository.JpaBikeRepository;
import com.sharecycle.domain.repository.JpaLedgerEntryRepository;
import com.sharecycle.infrastructure.persistence.JpaTripRepository;
import com.sharecycle.infrastructure.persistence.JpaUserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ListTripsUseCaseTest {
    @Autowired
    ListTripsUseCase listTripsUseCase;

    @Autowired
    private JpaLedgerEntryRepository jpaLedgerEntryRepository;

    @Autowired
    private JpaUserRepository userRepository;

    @Autowired
    private JpaTripRepository tripRepository;

    @Autowired
    private JpaBikeRepository bikeRepository;

    private Rider testUser;
    private Trip testTrip;
    private Bike testBike;

    @Test
    @Transactional
    void correctTripSearch() {
        createUser();
        createBike();
        createTrip();

        List<Trip> returnedTrip = listTripsUseCase.execute(testUser,
                LocalDateTime.of(2000, 1, 1, 0, 0),
                LocalDateTime.of(2100, 1, 1, 0, 0),
                Bike.BikeType.E_BIKE);
        assertEquals(1, returnedTrip.size());
    }

    @Test
    @Transactional
    void incorrectTripSearch() {
        createUser();
        createBike();
        createTrip();
        List<Trip> returnedTrip = listTripsUseCase.execute(testUser,
                LocalDateTime.of(1999, 1, 1, 0, 0),
                LocalDateTime.of(1999, 2, 1, 0, 0),
                Bike.BikeType.E_BIKE);
        assertEquals(0, returnedTrip.size());
    }


    void createUser() {
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
    void createBike() {
        testBike = new Bike();
        testBike.setId(UUID.randomUUID());
        testBike.setStatus(Bike.BikeStatus.AVAILABLE);
        testBike.setType(Bike.BikeType.E_BIKE);
        bikeRepository.save(testBike);
    }

    void createTrip() {
        testTrip = new Trip(UUID.randomUUID(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                testUser,
                testBike,
                new Station(),
                new Station());
        tripRepository.save(testTrip);
    }

}