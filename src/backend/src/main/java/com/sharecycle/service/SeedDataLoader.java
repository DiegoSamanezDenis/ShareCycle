package com.sharecycle.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sharecycle.domain.model.Bike;
import com.sharecycle.domain.model.Dock;
import com.sharecycle.domain.model.Station;
import com.sharecycle.domain.repository.JpaBikeRepository;
import com.sharecycle.domain.repository.JpaStationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Loads the canonical bike/station dataset from the JSON files under {@code db/data}.
 * This component can be reused both at bootstrap time and by operators who reset the system.
 */
@Service
public class SeedDataLoader {

    private static final Logger logger = LoggerFactory.getLogger(SeedDataLoader.class);
    private static final String BIKES_RESOURCE = "db/data/bikes.json";
    private static final String STATIONS_RESOURCE = "db/data/stations.json";

    private final ObjectMapper objectMapper;
    private final JpaBikeRepository bikeRepository;
    private final JpaStationRepository stationRepository;

    public SeedDataLoader(ObjectMapper objectMapper,
                          JpaBikeRepository bikeRepository,
                          JpaStationRepository stationRepository) {
        this.objectMapper = objectMapper;
        this.bikeRepository = bikeRepository;
        this.stationRepository = stationRepository;
    }

    public SeedResult reloadFromSeedFiles() {
        Map<UUID, Bike> bikes = loadBikes();
        bikes.values().forEach(bikeRepository::save);

        List<Station> stations = loadStations(bikes);
        stations.forEach(stationRepository::save);

        int dockCount = stations.stream()
                .mapToInt(station -> station.getDocks().size())
                .sum();

        return new SeedResult(bikes.size(), stations.size(), dockCount);
    }

    private Map<UUID, Bike> loadBikes() {
        try (InputStream inputStream = new ClassPathResource(BIKES_RESOURCE).getInputStream()) {
            List<BikeSeed> seeds = objectMapper.readValue(inputStream, new TypeReference<>() {});
            Map<UUID, Bike> bikes = new HashMap<>();
            for (BikeSeed seed : seeds) {
                Bike bike = new Bike(
                        seed.id(),
                        Bike.BikeType.valueOf(seed.type()),
                        Bike.BikeStatus.valueOf(seed.status()),
                        seed.reservationExpiry(),
                        null
                );
                bikes.put(bike.getId(), bike);
            }
            logger.info("Loaded {} bikes from {}", bikes.size(), BIKES_RESOURCE);
            return bikes;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load bikes seed data", e);
        }
    }

    private List<Station> loadStations(Map<UUID, Bike> bikes) {
        try (InputStream inputStream = new ClassPathResource(STATIONS_RESOURCE).getInputStream()) {
            List<StationSeed> seeds = objectMapper.readValue(inputStream, new TypeReference<>() {});
            List<Station> stations = new ArrayList<>();
            for (StationSeed seed : seeds) {
                Station.StationStatus status = Station.StationStatus.valueOf(seed.status());
                Station station = new Station(
                        seed.id(),
                        seed.name(),
                        status,
                        seed.latitude(),
                        seed.longitude(),
                        seed.address() == null ? "" : seed.address(),
                        seed.capacity(),
                        seed.bikesDocked()
                );

                List<Dock> docks = new ArrayList<>();
                List<UUID> dockBikeIds = new ArrayList<>();
                List<DockSeed> dockSeeds = seed.docks() != null ? seed.docks() : List.of();
                for (DockSeed dockSeed : dockSeeds) {
                    Dock dock = new Dock(dockSeed.id(), Dock.DockStatus.valueOf(dockSeed.status()), null);
                    docks.add(dock);
                    dockBikeIds.add(dockSeed.occupiedBike() != null ? dockSeed.occupiedBike().id() : null);
                }

                station.setDocks(docks);
                for (int i = 0; i < docks.size(); i++) {
                    UUID occupantId = dockBikeIds.get(i);
                    if (occupantId == null) {
                        continue;
                    }
                    Bike bike = bikes.get(occupantId);
                    if (bike == null) {
                        logger.warn("Dock {} references missing bike {}", docks.get(i).getId(), occupantId);
                        continue;
                    }
                    bike.setCurrentStation(station);
                    docks.get(i).setOccupiedBike(bike);
                }

                stations.add(station);
            }
            logger.info("Loaded {} stations from {}", stations.size(), STATIONS_RESOURCE);
            return stations;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load stations seed data", e);
        }
    }

    public record SeedResult(int bikes, int stations, int docks) {}

    private record BikeSeed(UUID id, String type, String status, Instant reservationExpiry) {}

    private record StationSeed(UUID id,
                               String name,
                               String status,
                               double latitude,
                               double longitude,
                               int bikesDocked,
                               int capacity,
                               String address,
                               List<DockSeed> docks) {}

    private record DockSeed(UUID id, String status, BikeSeed occupiedBike) {}
}

