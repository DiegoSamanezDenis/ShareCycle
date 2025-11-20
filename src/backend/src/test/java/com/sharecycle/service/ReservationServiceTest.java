package com.sharecycle.service;

import com.sharecycle.domain.ReservationBuilder;
import com.sharecycle.domain.model.Bike;
import com.sharecycle.domain.model.Rider;
import com.sharecycle.domain.model.Reservation;
import com.sharecycle.domain.model.Station;
import com.sharecycle.domain.model.User;
import com.sharecycle.domain.repository.JpaBikeRepository;
import com.sharecycle.domain.repository.JpaStationRepository;
import com.sharecycle.domain.repository.ReservationRepository;
import com.sharecycle.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JpaStationRepository jpaStationRepository;

    @Mock
    private JpaBikeRepository jpaBikeRepository;

    @InjectMocks
    private ReservationService reservationService;

    @Captor
    private ArgumentCaptor<Reservation> reservationCaptor;

    private UUID riderId;
    private UUID stationId;
    private UUID bikeId;

    @BeforeEach
    void setUp() {
        riderId = UUID.randomUUID();
        stationId = UUID.randomUUID();
        bikeId = UUID.randomUUID();
    }

    @Test
    void reserveBike_success_savesReservationAndReturnsIt() {
        // Arrange
        Rider riderMock = mock(Rider.class);
        Station station = new Station(stationId, "Central", Station.StationStatus.EMPTY, 0.0, 0.0, "addr", 0, 0);
        Bike bikeMock = mock(Bike.class);

        given(userRepository.findById(riderId)).willReturn(riderMock);
        given(jpaStationRepository.findById(stationId)).willReturn(station);
        given(jpaBikeRepository.findById(bikeId)).willReturn(bikeMock);

        // Act
        Reservation result = reservationService.reserveBike(riderId, stationId, bikeId, 10);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getRider()).isSameAs(riderMock);
        assertThat(result.getStation()).isSameAs(station);
        assertThat(result.getBike()).isSameAs(bikeMock);
        assertThat(result.isMarkedActive()).isTrue();

        // verify persisted
        verify(reservationRepository, times(1)).save(reservationCaptor.capture());
        Reservation saved = reservationCaptor.getValue();
        assertThat(saved.getReservationId()).isEqualTo(result.getReservationId());
        assertThat(saved.getRider()).isSameAs(riderMock);
    }

    @Test
    void reserveBike_whenUserIsNotRider_throwsIllegalStateException() {
        // Arrange
        User notRiderMock = mock(User.class); // not instance of Rider
        given(userRepository.findById(riderId)).willReturn(notRiderMock);
        given(jpaStationRepository.findById(stationId)).willReturn(new Station());
        given(jpaBikeRepository.findById(bikeId)).willReturn(mock(Bike.class));

        // Act / Assert
        assertThatThrownBy(() -> reservationService.reserveBike(riderId, stationId, bikeId, 5))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("is not a rider");

        verify(reservationRepository, never()).save(any());
    }
}
