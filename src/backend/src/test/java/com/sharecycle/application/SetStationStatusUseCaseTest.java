package com.sharecycle.application;

import com.sharecycle.domain.repository.JpaStationRepository;
import com.sharecycle.domain.repository.UserRepository;
import com.sharecycle.domain.model.*;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SetStationStatusUseCaseTest {

    @Autowired
    private SetStationStatusUseCase setStationStatusUseCase;

    @Autowired
    private JpaStationRepository stationRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @Transactional
    void togglesStationStatusAndCapacity() {
        Operator operator = createOperator();
        userRepository.save(operator);

        Station station = new Station();
        station.setName("Test Station");
        station.setLatitude(45.1);
        station.setLongitude(-73.4);
        station.setAddress("Test Address");
        station.markActive();
        station.addEmptyDocks(4);
        station.getDocks().get(0).setOccupiedBike(new Bike(Bike.BikeType.STANDARD));
        station.getDocks().get(1).setOccupiedBike(new Bike(Bike.BikeType.STANDARD));
        stationRepository.save(station);

        setStationStatusUseCase.execute(new SetStationStatusUseCase.Command(
                operator.getUserId(),
                station.getId(),
                true
        ));

        Station outOfService = stationRepository.findById(station.getId());
        assertThat(outOfService.isOutOfService()).isTrue();

        setStationStatusUseCase.execute(new SetStationStatusUseCase.Command(
                operator.getUserId(),
                station.getId(),
                false
        ));

        Station reactivated = stationRepository.findById(station.getId());
        assertThat(reactivated.isOutOfService()).isFalse();
        assertThat(reactivated.getCapacity()).isEqualTo(4);
        assertThat(reactivated.getStatus()).isEqualTo(Station.StationStatus.OCCUPIED);
    }

    private Operator createOperator() {
        Operator operator = new Operator();
        operator.setFullName("Casey Operator");
        operator.setStreetAddress("45 Admin Rd");
        operator.setEmail("casey.operator@example.com");
        operator.setUsername("op2");
        operator.setPasswordHash("hash");
        return operator;
    }
}
