package com.sharecycle.application;

import com.sharecycle.domain.model.*;
import com.sharecycle.domain.repository.JpaBikeRepository;
import com.sharecycle.domain.repository.JpaLedgerEntryRepository;
import com.sharecycle.infrastructure.SimpleDomainEventPublisher;
import com.sharecycle.infrastructure.persistence.JpaTripRepository;
import com.sharecycle.infrastructure.persistence.JpaUserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class PaymentUseCaseTest {
    @Autowired
    PaymentUseCase paymentUseCase;

    @Autowired
    private JpaLedgerEntryRepository jpaLedgerEntryRepository;

    @Autowired
    private JpaUserRepository userRepository;

    @Autowired
    private JpaTripRepository tripRepository;

    @Autowired
    private JpaBikeRepository bikeRepository;

    private Rider testUser;
    private LedgerEntry testLedgerEntry;
    private Trip testTrip;
    private Bike testBike;

    @Test
    void execute() {
    }

    @Test
    @Transactional
    void paymentSuccess() {
//        createUser();
//        createBike();
//        createTrip();
//        createLedgerEntry();
//        paymentUseCase.execute(testLedgerEntry);
//        LedgerEntry managedLedgerEntry = jpaLedgerEntryRepository.findById(testLedgerEntry.getLedgerId());
//        assertEquals(LedgerEntry.LedgerStatus.PAID, managedLedgerEntry.getLedgerStatus());
    }
    @Test
    @Transactional
    void paymentAlreadyPaid() {
//        createUser();
//        createBike();
//        createTrip();
//        createLedgerEntry();
//        testLedgerEntry.setLedgerStatus(LedgerEntry.LedgerStatus.PAID);
//        jpaLedgerEntryRepository.save(testLedgerEntry);
//        paymentUseCase.execute(testLedgerEntry);
//        LedgerEntry managedLedgerEntry = jpaLedgerEntryRepository.findById(testLedgerEntry.getLedgerId());
//        assertEquals(LedgerEntry.LedgerStatus.PAID, managedLedgerEntry.getLedgerStatus());
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
                (Rider) testUser,
                testBike,
                new Station(),
                new Station());
        tripRepository.save(testTrip);
    }


    void createLedgerEntry() {
        testLedgerEntry = new LedgerEntry();
        testLedgerEntry.setLedgerId(UUID.randomUUID());
        testLedgerEntry.setLedgerStatus(LedgerEntry.LedgerStatus.PENDING);
        testLedgerEntry.setTimestamp(Instant.now());
        testLedgerEntry.setUser(testUser);
        testLedgerEntry.setTrip(testTrip);
        testLedgerEntry.setTotalAmount(10.00);
        jpaLedgerEntryRepository.save(testLedgerEntry);
    }

}