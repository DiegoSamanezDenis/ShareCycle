package com.sharecycle.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sharecycle.domain.repository.JpaBikeRepository;
import com.sharecycle.domain.repository.JpaStationRepository;
import com.sharecycle.domain.model.Bike;
import com.sharecycle.domain.model.Station;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service
@Profile("!test")
public class TempNameDataSeeder {
    private final Logger logger = LoggerFactory.getLogger(TempNameDataSeeder.class);

    private final JpaBikeRepository bikeRepository;
    private final JpaStationRepository stationRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public TempNameDataSeeder(JpaBikeRepository bikeRepository, JpaStationRepository stationRepository) {
        this.bikeRepository = bikeRepository;
        this.stationRepository = stationRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initData(){
        logger.info("Loading seed data from classpath: db/data/*.json");
        // Load bikes first so station docks with occupied bikes reference existing rows
        loadBikes();
        // Then load stations; docks will be persisted via cascade with station_id set
        loadStations();
        logger.info("Seed data loaded");
    }

    private void loadBikes() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("db/data/bikes.json")) {
            if (is == null) {
                logger.warn("bikes.json not found on classpath; skipping bikes seeding");
                return;
            }
            List<Bike> bikes = objectMapper.readValue(is, new TypeReference<>() {});
            List<Bike> existingBikes = bikeRepository.findAll();
            for (Bike bike : bikes) {
                if (!existingBikes.contains(bike)) {
                    bikeRepository.save(bike);
                }
            }
        } catch (IOException e) {
            logger.error("Failed to load bikes data file");
            logger.error(e.getMessage());
        }
    }
    // Docks are persisted through stations' docks association via cascade;
    // do not load docks independently.
    private void loadStations() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("db/data/stations.json")) {
            if (is == null) {
                logger.warn("stations.json not found on classpath; skipping stations seeding");
                return;
            }
            List<Station> stations = objectMapper.readValue(is, new TypeReference<>() {});
            List<Station> existingStations = stationRepository.findAll();
            for (Station station : stations) {
                if (!existingStations.contains(station)) {
                    stationRepository.save(station);
                }
            }
        } catch (IOException e) {
            logger.error("Failed to load stations data file");
        }

    }

}
