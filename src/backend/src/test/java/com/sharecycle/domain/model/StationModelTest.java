package com.sharecycle.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class StationModelTest {

    @Test
    void getFullnessCategory_whenCapacityZero_returnsUNKNOWN() {
        Station s = new Station(UUID.randomUUID(), "A", Station.StationStatus.EMPTY, 0.0, 0.0, "addr", 0, 0);
        assertThat(s.getFullnessCategory()).isEqualTo("UNKNOWN");
    }

    @Test
    void getFullnessCategory_emptyLowHealthyFull() {
        Station s = new Station(UUID.randomUUID(), "A", Station.StationStatus.EMPTY, 0.0, 0.0, "addr", 10, 0);
        s.setBikesDocked(0);
        assertThat(s.getFullnessCategory()).isEqualTo("EMPTY");

        s.setBikesDocked(2); // 20%
        assertThat(s.getFullnessCategory()).isEqualTo("LOW");

        s.setBikesDocked(5); // 50%
        assertThat(s.getFullnessCategory()).isEqualTo("HEALTHY");

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

        s.markActive(); // sets to EMPTY then sync from counts -> not out of service
        assertThat(s.isOutOfService()).isFalse();
        // since bikesDocked is 3 and capacity 10, status should be OCCUPIED
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
}
