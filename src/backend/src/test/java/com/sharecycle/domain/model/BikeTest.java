package com.sharecycle.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sharecycle.domain.model.Bike.BikeStatus;
import com.sharecycle.domain.model.Bike.BikeType;

class BikeTest {

    private Bike bike;

    @BeforeEach
    void setup() {
        bike = new Bike();
    }

    @Test
    void testDefaultConstructor() {
        assertNotNull(bike.getId(), "Bike ID should not be null");
        assertEquals(BikeType.STANDARD, bike.getType(), "Default type should be STANDARD");
        assertEquals(BikeStatus.AVAILABLE, bike.getStatus(), "Default status should be AVAILABLE");
        assertNull(bike.getCurrentStation(), "Default station should be null");
        assertNull(bike.getReservationExpiry(), "Default reservation expiry should be null");
    }

    @Test
    void testConstructorWithType() {
        Bike eBike = new Bike(BikeType.E_BIKE);
        assertEquals(BikeType.E_BIKE, eBike.getType());
        assertEquals(BikeStatus.AVAILABLE, eBike.getStatus());
    }

    @Test
    void testFullConstructor() {
        UUID id = UUID.randomUUID();
        Instant expiry = Instant.now();
        Station station = new Station();
        Bike b = new Bike(id, BikeType.E_BIKE, BikeStatus.RESERVED, expiry, station);

        assertEquals(id, b.getId());
        assertEquals(BikeType.E_BIKE, b.getType());
        assertEquals(BikeStatus.RESERVED, b.getStatus());
        assertEquals(expiry, b.getReservationExpiry());
        assertEquals(station, b.getCurrentStation());
    }

    @Test
    void testSettersAndGetters() {
        UUID id = UUID.randomUUID();
        bike.setId(id);
        assertEquals(id, bike.getId());

        bike.setType(BikeType.E_BIKE);
        assertEquals(BikeType.E_BIKE, bike.getType());

        Instant expiry = Instant.now();
        bike.setReservationExpiry(expiry);
        assertEquals(expiry, bike.getReservationExpiry());

        Station station = new Station();
        bike.setCurrentStation(station);
        assertEquals(station, bike.getCurrentStation());
    }

    @Test
    void testReserve() {
        bike.reserve();
        assertEquals(BikeStatus.RESERVED, bike.getStatus());
    }

    @Test
    void testCheckout() {
        bike.reserve();
        bike.checkout();
        assertEquals(BikeStatus.ON_TRIP, bike.getStatus());
    }

    @Test
    void testCompleteTrip() {
        bike.checkout();
        bike.completeTrip();
        assertEquals(BikeStatus.AVAILABLE, bike.getStatus());
    }

    @Test
    void testMarkAvailable() {
        bike.sendToMaintenance();
        bike.markAvailable();
        assertEquals(BikeStatus.AVAILABLE, bike.getStatus());
    }

    @Test
    void testSendToMaintenance() {
        bike.sendToMaintenance();
        assertEquals(BikeStatus.MAINTENANCE, bike.getStatus());
    }
}
