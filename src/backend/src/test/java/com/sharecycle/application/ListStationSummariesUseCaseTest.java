package com.sharecycle.application;

import com.sharecycle.domain.model.Bike;
import com.sharecycle.domain.model.Station;
import com.sharecycle.domain.repository.JpaStationRepository;
import com.sharecycle.model.dto.StationSummaryDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(classes = {PaymentGatewayTestConfig.class}) // <- include test PaymentGateway bean
class ListStationSummariesUseCaseTest {

    @Autowired
    private ListStationSummariesUseCase useCase;

    @Autowired
    private JpaStationRepository stationRepository;

    private Station station;

    @BeforeEach
    void setUp() {
        // Clear the repository if you have a test method for that, otherwise ensure DB is clean

        station = new Station();
        station.setName("Summary Station");
        station.setLatitude(45.1234);
        station.setLongitude(-73.5678);
        station.markActive();
        station.addEmptyDocks(3);
        station.getDocks().get(0).setOccupiedBike(new Bike(Bike.BikeType.STANDARD));
        station.getDocks().get(1).setOccupiedBike(new Bike(Bike.BikeType.E_BIKE));
        stationRepository.save(station);
    }

    @Test
    void stationSummaryIncludesCoordinatesAndFullness() {
        List<StationSummaryDto> summaries = useCase.execute();

        StationSummaryDto dto = summaries.stream()
                .filter(s -> s.getStationId().equals(station.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(dto.getLatitude()).isEqualTo(45.1234);
        assertThat(dto.getLongitude()).isEqualTo(-73.5678);
        assertThat(dto.getBikesAvailable()).isEqualTo(2);
        assertThat(dto.getFullnessCategory()).isEqualTo("HEALTHY");
        assertThat(dto.getFreeDocks()).isEqualTo(1);
        assertThat(dto.getEBikesDocked()).isEqualTo(1);
        assertThat(dto.getEBikesAvailable()).isEqualTo(1);
    }
}
