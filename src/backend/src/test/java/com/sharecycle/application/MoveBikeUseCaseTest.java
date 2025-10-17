package com.sharecycle.application;

import com.sharecycle.domain.repository.JpaBikeRepository;
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

@SpringBootTest
@ActiveProfiles("test")
class MoveBikeUseCaseTest {

    @Autowired
    private MoveBikeUseCase moveBikeUseCase;

    @Autowired
    private JpaStationRepository stationRepository;

    @Autowired
    private JpaBikeRepository bikeRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @Transactional
    void movesBikeBetweenStationsAndUpdatesCounts() {
        Operator operator = createOperator();
        userRepository.save(operator);

        Station source = createStationWithDocks("Source Station", 45.0, -73.0, 5);
        Station destination = createStationWithDocks("Destination Station", 46.0, -74.0, 5);

        Bike bike = new Bike(Bike.BikeType.STANDARD);
        source.getDocks().get(0).setOccupiedBike(bike);

        stationRepository.save(source);
        stationRepository.save(destination);

        moveBikeUseCase.execute(new MoveBikeUseCase.Command(
                operator.getUserId(),
                bike.getId(),
                destination.getId()
        ));

        Bike updatedBike = bikeRepository.findById(bike.getId());
        Station updatedSource = stationRepository.findById(source.getId());
        Station updatedDestination = stationRepository.findById(destination.getId());

        assertThat(updatedBike.getCurrentStation().getId()).isEqualTo(destination.getId());
        assertThat(updatedSource.getBikesDocked()).isZero();
        assertThat(updatedSource.getStatus()).isEqualTo(Station.StationStatus.EMPTY);
        assertThat(updatedDestination.getBikesDocked()).isEqualTo(1);
        assertThat(updatedDestination.getStatus()).isEqualTo(Station.StationStatus.OCCUPIED);
    }

    private Operator createOperator() {
        Operator operator = new Operator();
        operator.setFullName("Olivia Operator");
        operator.setStreetAddress("123 Ops Street");
        operator.setEmail("operator@example.com");
        operator.setUsername("operator1");
        operator.setPasswordHash("hash");
        return operator;
    }

    private Station createStationWithDocks(String name, double latitude, double longitude, int capacity) {
        Station station = new Station();
        station.setName(name);
        station.setLatitude(latitude);
        station.setLongitude(longitude);
        station.setAddress("Unknown");
        station.markActive();
        station.addEmptyDocks(capacity);
        return station;
    }
}
