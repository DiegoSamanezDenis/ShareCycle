package com.sharecycle.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sharecycle.application.RegisterRiderUseCase;
import com.sharecycle.domain.model.Bike;
import com.sharecycle.domain.model.Station;
import com.sharecycle.domain.model.Trip;
import com.sharecycle.domain.model.User;
import com.sharecycle.domain.repository.JpaBikeRepository;
import com.sharecycle.domain.repository.JpaStationRepository;
import com.sharecycle.domain.repository.TripRepository;
import com.sharecycle.domain.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
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
    private final RegisterRiderUseCase registerRiderUseCase;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public TempNameDataSeeder(JpaBikeRepository bikeRepository,
                              JpaStationRepository stationRepository,
                              UserRepository userRepository,
                              TripRepository tripRepository,
                              RegisterRiderUseCase registerRiderUseCase) {
        this.bikeRepository = bikeRepository;
        this.stationRepository = stationRepository;
        this.userRepository = userRepository;
        this.tripRepository = tripRepository;
        this.registerRiderUseCase = registerRiderUseCase;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initData() {
        logger.info("Loading seed data...");

        loadBikes();
        loadStations();

        createBronzeRider();
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

        // Guaranteed to work because loadBikes() ran first
        seedTrips(result.userId(), 12, LocalDateTime.now().minusWeeks(2));
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
}