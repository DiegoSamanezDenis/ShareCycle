package com.sharecycle.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sharecycle.application.RegisterOperatorUseCase;
import com.sharecycle.application.RegisterRiderUseCase;
import com.sharecycle.domain.model.*;
import com.sharecycle.domain.repository.*;
import com.sharecycle.infrastructure.persistence.JpaLoyaltyRepositoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@Profile("!test")
public class TempNameDataSeeder {
    private final Logger logger = LoggerFactory.getLogger(TempNameDataSeeder.class);

    // Infrastructure Repos
    private final JpaBikeRepository bikeRepository;
    private final JpaStationRepository stationRepository;

    // User & Trip Repos
    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final ReservationRepository reservationRepository;
    private final RegisterOperatorUseCase registerOperatorUseCase;
    private final RegisterRiderUseCase registerRiderUseCase;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JpaLoyaltyRepositoryImpl jpaLoyaltyRepositoryImpl;

    public TempNameDataSeeder(JpaBikeRepository bikeRepository,
                              JpaStationRepository stationRepository,
                              UserRepository userRepository,
                              TripRepository tripRepository,
                              RegisterOperatorUseCase registerOperatorUseCase,
                              RegisterRiderUseCase registerRiderUseCase, ReservationRepository reservationRepository, JpaLoyaltyRepositoryImpl jpaLoyaltyRepositoryImpl) {
        this.bikeRepository = bikeRepository;
        this.stationRepository = stationRepository;
        this.userRepository = userRepository;
        this.tripRepository = tripRepository;
        this.registerOperatorUseCase = registerOperatorUseCase;
        this.registerRiderUseCase = registerRiderUseCase;
        this.reservationRepository = reservationRepository;
        this.jpaLoyaltyRepositoryImpl = jpaLoyaltyRepositoryImpl;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initData() {
        logger.info("Loading seed data...");

        // 1. Load Infrastructure FIRST
        loadBikes();
        loadStations();

        // 2. Create Users & History
        createOperator();
        createBronzeRider();     // The existing 12-trip user
        createPreBronzeRider();
        createPreSilverRider();
        createPreGoldRider();
        downgradeGoldtoSilver();
        downgradeGoldtoBronze();
    }

    private void loadBikes() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("db/data/bikes.json")) {
            if (is == null) return;
            List<Bike> bikes = objectMapper.readValue(is, new TypeReference<>() {});
            for (Bike bike : bikes) {
                if (bikeRepository.findById(bike.getId()) == null) {
                    bikeRepository.save(bike);
                }
            }
        } catch (IOException e) {
            logger.error("Failed to load bikes", e);
        }
    }

    private void loadStations() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("db/data/stations.json")) {
            if (is == null) return;
            List<Station> stations = objectMapper.readValue(is, new TypeReference<>() {});
            for (Station station : stations) {
                if (stationRepository.findById(station.getId()) == null) {
                    stationRepository.save(station);
                }
            }
        } catch (IOException e) {
            logger.error("Failed to load stations", e);
        }
    }

    private void createOperator() {
        if (!userRepository.existsByUsername("smoothoperator")) {
            registerOperatorUseCase.register(
                    "Smooth Operator",
                    "100 Demo Street, Montreal, QC",
                    "smooth@sharecycle.com",
                    "SmoothOperator",
                    "wowpass",
                    null
            );
        }
    }

    private void createBronzeRider() {
        String username = "bronze_rider";
        if (userRepository.existsByUsername(username)) return;

        RegisterRiderUseCase.RegistrationResult result = registerRiderUseCase.register(
                "Bronze Tester",
                "3rd Place Ave",
                "bronze@sharecycle.com",
                username,
                "password",
                "pm_card_visa",
                "PAY_AS_YOU_GO"
        );

        // Seed 12 trips (Already Bronze)
        seedTrips(result.userId(), 12, LocalDateTime.now().minusWeeks(2));
    }

    // --- NEW METHOD ---
    private void createPreBronzeRider() {
        String username = "entry_rider";
        if (userRepository.existsByUsername(username)) return;

        RegisterRiderUseCase.RegistrationResult result = registerRiderUseCase.register(
                "Entry To Bronze",
                "123 Entry Blvd",
                "entry@sharecycle.com",
                username,
                "password",
                "pm_card_visa",
                "PAY_AS_YOU_GO"
        );

        // Seed 9 trips (1 short of Bronze)
        seedTrips(result.userId(), 9, LocalDateTime.now().minusWeeks(2));
    }

    private void createPreSilverRider() {
        String username = "silver_rider";
        if (userRepository.existsByUsername(username)) return;

        RegisterRiderUseCase.RegistrationResult result = registerRiderUseCase.register(
                "Silver Hopeful", "456 Silver St", "silver@sharecycle.com",
                username, "password", "pm_card_visa", "PAY_AS_YOU_GO"
        );

        UUID userId = result.userId();

        // 1. Seed Consistency
        // Month -2: 6 Trips (PASS)
        seedTrips(userId, 6, LocalDateTime.now().minusMonths(2));

        // Month -1: 6 Trips (PASS)
        seedTrips(userId, 6, LocalDateTime.now().minusMonths(1));

        // Current Month: 4 Trips (FAIL - Needs 1 more to reach 5)
        seedTrips(userId, 4, LocalDateTime.now().minusHours(24));

        // 2. Seed Reservations (Target: At least 5)
        // Create 4 Reservations (FAIL - Needs 1 more to reach 5)
        seedReservations(userId, 4);

    }

    private void createPreGoldRider() {
        String username = "gold_rider";
        if (userRepository.existsByUsername(username)) return;

        RegisterRiderUseCase.RegistrationResult result = registerRiderUseCase.register(
                "Gold Hopeful", "789 Gold Rd", "gold@sharecycle.com",
                username, "password", "pm_card_visa", "PAY_AS_YOU_GO"
        );
        UUID userId = result.userId();

        // 1. Seed Reservations
        seedReservations(userId, 10);

        // 2. Seed History: 12 weeks of solid 6 trips/week
        LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);

        for (int i = 0; i < 12; i++) {
            LocalDateTime weekDate = threeMonthsAgo.plusWeeks(i);
            seedTrips(userId, 6, weekDate);
        }

        // 3. Seed CURRENT Week: 5 trips TODAY
        seedTrips(userId, 4, LocalDateTime.now().minusHours(4));

        logger.info(">>> SEEDED PRE-GOLD: 12 weeks of history + 5 trips TODAY.");
    }

    private void downgradeGoldtoSilver(){
        String username = "gold_to_silver";
        if (userRepository.existsByUsername(username)) return;

        RegisterRiderUseCase.RegistrationResult result = registerRiderUseCase.register(
                "Gold to Silver", "101 Change Ln", "goldtosilver@sharecycle.com",
                username, "password", "pm_card_visa", "PAY_AS_YOU_GO"
        );

        UUID userId = result.userId();

        // 1. Reservations: Enough for Gold (10)
        seedReservations(userId, 10);

        // 2. Recent History: Strong (6 trips/week for last 2 months)
        seedTrips(userId, 6, LocalDateTime.now().minusMonths(1));
        seedTrips(userId, 6, LocalDateTime.now().minusMonths(2));

        // 3. THE TRICK: The "Oldest" Week

        // Set timestamp to 3 months ago + 15 seconds buffer
        LocalDateTime edgeOfWindow = LocalDateTime.now().minusMonths(3).plusSeconds(90);

        seedTrips(userId, 6, edgeOfWindow);

        logger.info(">>> SEEDED GOLD->SILVER TIME BOMB: User 'gold_to_bronze' will lose Gold status in ~90 seconds.");
    }

    private void downgradeGoldtoBronze() {
        String username = "gold_to_bronze";
        if (userRepository.existsByUsername(username)) return;

        RegisterRiderUseCase.RegistrationResult result = registerRiderUseCase.register(
                "Bronze Fall", "1 Cliff Edge", "goldtobronze@sharecycle.com",
                username, "password", "pm_card_visa", "PAY_AS_YOU_GO"
        );
        UUID userId = result.userId();

        // 1. Reservations: 6 (Just enough for Silver/Gold)
        seedReservations(userId, 6);

        // 2. Recent History: Strong
        seedTrips(userId, 6, LocalDateTime.now().minusMonths(1));
        seedTrips(userId, 6, LocalDateTime.now().minusMonths(2));

        // 3. THE TIME BOMB (Month -3)
        // We place 6 trips exactly at the 3-month cutoff + 90 seconds buffer.
        // RIGHT NOW: They are inside the window. (User has 3 months of history -> Gold)
        // IN 90 SECS: They fall out. (User has 2 months of history -> Fails Silver -> Bronze)
        LocalDateTime edgeOfWindow = LocalDateTime.now().minusMonths(3).plusSeconds(90);
        seedTrips(userId, 6, edgeOfWindow);

        logger.info(">>> SEEDED GOLD->BRONZE TIME BOMB: User 'gold_to_bronze' will drop to Bronze in ~90 seconds.");
    }

    private void seedTrips(UUID userId, int count, LocalDateTime startDate) {
        User user = userRepository.findById(userId);
        Bike bike = bikeRepository.findAll().stream().findFirst().orElse(null);
        Station station = stationRepository.findAll().stream().findFirst().orElse(null);

        if (user == null || bike == null || station == null) {
            logger.warn("Skipping trip seeding: Missing user, bike, or station.");
            return;
        }

        for (int i = 0; i < count; i++) {
            LocalDateTime start = startDate.plusHours(i * 12L);
            LocalDateTime end = start.plusMinutes(25);

            Trip trip = new Trip(
                    UUID.randomUUID(),
                    start,
                    end,
                    (com.sharecycle.domain.model.Rider) user,
                    bike,
                    station,
                    station
            );
            tripRepository.save(trip);
        }
        logger.info(">>> SEEDED {} TRIPS FOR USER: {}", count, userId);
    }

    private void seedReservations(UUID userId, int count) {
        User user = userRepository.findById(userId);
        Bike bike = bikeRepository.findAll().stream().findFirst().orElse(null);
        Station station = stationRepository.findAll().stream().findFirst().orElse(null);

        if (user == null || bike == null || station == null) return;

        for (int i = 0; i < count; i++) {
            Reservation r = new Reservation(
                    UUID.randomUUID(),
                    (com.sharecycle.domain.model.Rider) user,
                    station,
                    bike,
                    Instant.now().minusSeconds(86400 * (i + 1)), // Days ago
                    Instant.now().minusSeconds(86400 * i), // Expired
                    15,
                    false // Inactive/Expired history
            );
            reservationRepository.save(r);
        }
        logger.info(">>> SEEDED {} RESERVATIONS FOR USER: {}", count, userId);
    }
}