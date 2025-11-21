package com.sharecycle.application;

import com.sharecycle.domain.event.DomainEventPublisher;
import com.sharecycle.domain.model.Bike;
import com.sharecycle.domain.model.Station;
import com.sharecycle.domain.model.User;
import com.sharecycle.domain.repository.JpaBikeRepository;
import com.sharecycle.domain.repository.JpaStationRepository;
import com.sharecycle.domain.repository.ReservationRepository;
import com.sharecycle.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Import;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Import(TripUseCaseTest.PaymentGatewayTestConfig.class)
class MoveBikeUseCaseGuardTest {

    @Mock
    private JpaBikeRepository bikeRepository;
    @Mock
    private JpaStationRepository stationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private DomainEventPublisher eventPublisher;

    private MoveBikeUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new MoveBikeUseCase(
                bikeRepository,
                stationRepository,
                userRepository,
                reservationRepository,
                eventPublisher
        );
    }

    @Test
    void cannotMoveBikeWithActiveReservation() {
        UUID operatorId = UUID.randomUUID();
        UUID bikeId = UUID.randomUUID();
        UUID destinationId = UUID.randomUUID();

        User operator = new User();
        operator.setUserId(operatorId);
        operator.setRole("OPERATOR");

        Bike bike = new Bike();
        bike.setId(bikeId);
        bike.setStatus(Bike.BikeStatus.AVAILABLE);

        Station sourceStation = new Station();
        sourceStation.setId(UUID.randomUUID());
        sourceStation.markActive();
        sourceStation.addEmptyDocks(1);
        sourceStation.getDocks().getFirst().setOccupiedBike(bike);
        bike.setCurrentStation(sourceStation);

        Station destinationStation = new Station();
        destinationStation.setId(destinationId);
        destinationStation.markActive();
        destinationStation.addEmptyDocks(1);

        when(userRepository.findById(operatorId)).thenReturn(operator);
        when(bikeRepository.findById(bikeId)).thenReturn(bike);
        when(stationRepository.findByIdForUpdate(sourceStation.getId())).thenReturn(sourceStation);
        when(stationRepository.findByIdForUpdate(destinationId)).thenReturn(destinationStation);
        when(reservationRepository.hasActiveReservationForBike(bikeId)).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(new MoveBikeUseCase.Command(
                operatorId,
                bikeId,
                destinationId
        ))).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("active reservation");

        verify(bikeRepository, never()).save(bike);
    }
}
