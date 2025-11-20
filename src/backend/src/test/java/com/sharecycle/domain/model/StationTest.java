package com.sharecycle.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class StationTest {

    @Test
    void getFullnessCategory_whenCapacityZero_returnsUNKNOWN() {
        Station s = new Station(UUID.randomUUID(), "A", Station.StationStatus.EMPTY, 0.0, 0.0, "addr", 0, 0);
        assertThat(s.getFullnessCategory()).isEqualTo("UNKNOWN");
    }

    @Test
    void getFullnessCategory_emptyLowHealthyFull() {
        Station s = new Station(UUID.randomUUID(), "A", Station.StationStatus.EMPTY, 0.0, 0.0, "addr", 10, 0);

        // EMPTY
        s.setBikesDocked(0);
        assertThat(s.getFullnessCategory()).isEqualTo("EMPTY");

        // LOW (<30%)
        s.setBikesDocked(2); // 20%
        assertThat(s.getFullnessCategory()).isEqualTo("LOW");

        // HEALTHY (0.3 <= ratio <= 0.9)
        s.setBikesDocked(5); // 50%
        assertThat(s.getFullnessCategory()).isEqualTo("HEALTHY");

        // FULL (>90%)
        s.setBikesDocked(10); // 100%
        assertThat(s.getFullnessCategory()).isEqualTo("FULL");
    }

    @Test
    void getFreeDockCount_and_hasAvailableBike_and_statusTransitions() {
        Station s = new Station(UUID.randomUUID(), "A", Station.StationStatus.EMPTY, 0.0, 0.0, "addr", 10, 3);

        assertThat(s.getFreeDockCount()).isEqualTo(7);
        assertThat(s.hasAvailableBike()).isTrue();

        s.markOutOfService();
        assertThat(s.isOutOfService()).isTrue();

        s.markActive(); // status set to EMPTY then synced from counts
        assertThat(s.isOutOfService()).isFalse();
        // bikesDocked = 3, capacity = 10 → status should be OCCUPIED
        assertThat(s.getStatus()).isEqualTo(Station.StationStatus.OCCUPIED);
    }

    @Test
    void addAndRemoveEmptyDocks_validatesAndRecalculatesCapacity() {
        Station s = new Station(UUID.randomUUID(), "B", Station.StationStatus.EMPTY, 0.0, 0.0, "addr", 0, 0);

        // add 3 empty docks
        s.addEmptyDocks(3);
        assertThat(s.getCapacity()).isEqualTo(3);

        // remove 2 empty docks
        s.removeEmptyDocks(2);
        assertThat(s.getCapacity()).isEqualTo(1);
    }

    @Test
    void addEmptyDocks_negative_throws() {
        Station s = new Station();
        assertThatThrownBy(() -> s.addEmptyDocks(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void removeEmptyDocks_tooMany_throws() {
        Station s = new Station();
        s.addEmptyDocks(1);
        assertThatThrownBy(() -> s.removeEmptyDocks(2))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void dockingBikes_updatesStatusAndCounts() {
        Station s = new Station();
        s.addEmptyDocks(5);

        Bike b1 = new Bike();
        Bike b2 = new Bike();

        s.dockBike(b1);
        s.dockBike(b2);

        assertThat(s.getBikesDocked()).isEqualTo(2);
        assertThat(s.getStatus()).isEqualTo(Station.StationStatus.OCCUPIED);
        assertThat(s.getAvailableBikeCount()).isEqualTo(2);

        // fill remaining docks
        for (int i = 0; i < 3; i++) {
            s.dockBike(new Bike());
        }
        assertThat(s.getStatus()).isEqualTo(Station.StationStatus.FULL);
        assertThat(s.getFreeDockCount()).isEqualTo(0);
    }
}
