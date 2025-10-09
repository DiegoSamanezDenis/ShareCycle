package com.sharecycle.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sharecycle.model.entity.Bike;
import com.sharecycle.model.entity.Dock;
import com.sharecycle.model.entity.Station;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DataGenerator {
    private static final Logger logger = LoggerFactory.getLogger(com.sharecycle.service.DataGenerator.class);
    public static void generateRandomDataFiles() {
        String dirPath = System.getProperty("user.dir") + "/src/main/resources/db/data";
        Random rand = new Random();
        List<Station> stations = new ArrayList<>();
        List<Dock> docks = new ArrayList<>();
        List<Bike> bikes = new ArrayList<>();
        // Generate random location around Montreal
        for (int i = 1; i <= 10; i++) { // 10 stations
            double latitude = 45.50 + rand.nextDouble() * (45.70 - 45.42);
            double longtitude = -73.63 + rand.nextDouble() * (-73.50 + 73.98);
            List<Dock> newDocks = new ArrayList<>();
            for (int j = 0; j < 5; j++) { //Each station has 5 dock
                Dock dock = new Dock();
                newDocks.add(dock);
                if (rand.nextBoolean()) { //Random if there's dock
                    Bike bike = new Bike();
                    dock.setOccupiedBike(bike);
                    bikes.add(bike);
                }
                docks.add(dock);
            }
            Station station = new Station();
            station.setName("Station #" + i);
            station.setDocks(newDocks);
            station.setLatitude(latitude);
            station.setLongitude(longtitude);
            stations.add(station);
        }
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        try {
            File file = new File(dirPath+"/stations.json");
            new ObjectMapper().writeValue(file, stations);
            file = new File(dirPath+"/docks.json");
            new ObjectMapper().writeValue(file, docks);
            file = new File(dirPath+"/bikes.json");
            new ObjectMapper().writeValue(file, bikes);
            logger.info("Generated stations, docks, bikes file");
        } catch (IOException e) {
            logger.error(e.getMessage());
        }

    }

}