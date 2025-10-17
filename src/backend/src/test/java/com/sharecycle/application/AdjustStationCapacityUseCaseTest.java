package com.sharecycle.application;

import com.sharecycle.domain.repository.JpaStationRepository;
import com.sharecycle.domain.repository.UserRepository;
import com.sharecycle.model.entity.Bike;
import com.sharecycle.model.entity.Operator;
import com.sharecycle.model.entity.Station;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class AdjustStationCapacityUseCaseTest {

    @Autowired
    private AdjustStationCapacityUseCase adjustStationCapacityUseCase;

    @Autowired
    private JpaStationRepository stationRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @Transactional
    void increasesCapacityByAddingEmptyDocks() {
        Operator operator = createOperator("operator-capacity-add");
        userRepository.save(operator);

        Station station = new Station();
        station.setName("Capacity Up Station");
        station.setLatitude(45.0);
        station.setLongitude(-73.0);
        station.setAddress("Address");
        station.markActive();
        station.addEmptyDocks(2);
        stationRepository.save(station);

        adjustStationCapacityUseCase.execute(new AdjustStationCapacityUseCase.Command(
                operator.getUserId(),
                station.getId(),
                3
        ));

        Station updated = stationRepository.findById(station.getId());
        assertThat(updated.getCapacity()).isEqualTo(5);
        assertThat(updated.getBikesDocked()).isZero();
        assertThat(updated.getFreeDockCount()).isEqualTo(5);
    }

    @Test
    @Transactional
    void decreasesCapacityByRemovingEmptyDocks() {
        Operator operator = createOperator("operator-capacity-remove");
        userRepository.save(operator);

        Station station = new Station();
        station.setName("Capacity Down Station");
        station.setLatitude(45.0);
        station.setLongitude(-73.0);
        station.setAddress("Address");
        station.markActive();
        station.addEmptyDocks(4);
        station.getDocks().get(0).setOccupiedBike(new Bike(Bike.BikeType.STANDARD));
        stationRepository.save(station);

        adjustStationCapacityUseCase.execute(new AdjustStationCapacityUseCase.Command(
                operator.getUserId(),
                station.getId(),
                -2
        ));

        Station updated = stationRepository.findById(station.getId());
        assertThat(updated.getCapacity()).isEqualTo(2);
        assertThat(updated.getBikesDocked()).isEqualTo(1);
        assertThat(updated.getFreeDockCount()).isEqualTo(1);
    }

    @Test
    @Transactional
    void failsWhenRemovingMoreDocksThanAvailable() {
        Operator operator = createOperator("operator-capacity-fail");
        userRepository.save(operator);

        Station station = new Station();
        station.setName("Capacity Fail Station");
        station.setLatitude(45.0);
        station.setLongitude(-73.0);
        station.setAddress("Address");
        station.markActive();
        station.addEmptyDocks(1);
        station.getDocks().get(0).setOccupiedBike(new Bike(Bike.BikeType.STANDARD));
        stationRepository.save(station);

        assertThatThrownBy(() -> adjustStationCapacityUseCase.execute(new AdjustStationCapacityUseCase.Command(
                operator.getUserId(),
                station.getId(),
                -1
        ))).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Not enough empty docks");
    }

    private Operator createOperator(String username) {
        Operator operator = new Operator();
        operator.setFullName("Operator " + username);
        operator.setStreetAddress("123 Ops Street");
        operator.setEmail(username + "@example.com");
        operator.setUsername(username);
        operator.setPasswordHash("hash");
        return operator;
    }
}
