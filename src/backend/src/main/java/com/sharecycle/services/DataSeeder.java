package com.sharecycle.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sharecycle.domain.repository.JpaBikeRepository;
import com.sharecycle.domain.repository.JpaDockRepository;
import com.sharecycle.domain.repository.JpaStationRepository;
import com.sharecycle.model.entity.Bike;
import com.sharecycle.model.entity.Dock;
import com.sharecycle.model.entity.Station;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Service
public class DataSeeder {
    private final Logger logger = LoggerFactory.getLogger(DataSeeder.class);

    private final JpaBikeRepository bikeRepository;
    private final JpaDockRepository dockRepository;
    private final JpaStationRepository stationRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String dirPath = System.getProperty("user.dir") +  "/src/main/resources/db/data";

    public DataSeeder(JpaBikeRepository bikeRepository, JpaDockRepository dockRepository, JpaStationRepository stationRepository) {
        this.bikeRepository = bikeRepository;
        this.dockRepository = dockRepository;
        this.stationRepository = stationRepository;
    }

    @PostConstruct
    private void init(){ String dirPath = System.getProperty("user.dir") + "/src/main/resources/db/data";
        System.out.println(dirPath);
        if (new File(dirPath+"/stations.json").exists()) {
            logger.info("Data files already existed");
        } else {
            logger.warn("Data files not existed");
            logger.info("Creating random data files");
            com.sharecycle.service.DataGenerator.generateRandomDataFiles();
        }
        loadBikes();
        loadDocks();
        loadStations();
    }

    private void loadBikes() {

        File bikeFile = new File(dirPath + "/bikes.json");
        try {
            List<Bike> bikes = objectMapper.readValue(
                    bikeFile,
                    new TypeReference<>() {
                    }
            );
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
    private void loadDocks() {
        File dockFile = new File(dirPath + "/docks.json");
        try {

            List<Dock> docks = objectMapper.readValue(
                    dockFile,
                    new TypeReference<>() {
                    }
            );
            List<Dock> existingDocks = dockRepository.findAll();
            for (Dock dock : docks) {
                if (!existingDocks.contains(dock)) {
                    dockRepository.save(dock);
                }
            }
        } catch (IOException e) {
            logger.error("Failed to load docks data file");
            logger.error(e.getMessage());
        }
    }
    private void loadStations() {
        try {
            File stationFile = new File(dirPath + "/stations.json");
            List<Station> stations = objectMapper.readValue(
                    stationFile,
                    new TypeReference<>() {
                    }
            );
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
