package com.sharecycle.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sharecycle.domain.model.Bike;
import com.sharecycle.domain.model.Reservation;
import com.sharecycle.domain.model.Rider;
import com.sharecycle.domain.model.Station;
import com.sharecycle.domain.repository.JpaBikeRepository;
import com.sharecycle.domain.repository.JpaLedgerEntryRepository;
import com.sharecycle.domain.repository.JpaStationRepository;
import com.sharecycle.domain.repository.ReservationRepository;
import com.sharecycle.domain.repository.TripRepository;
import com.sharecycle.domain.repository.UserRepository;
import com.sharecycle.model.dto.StationDetailsDto;

@ExtendWith(MockitoExtension.class)
public class BmsFacadeStationDetailsTest {

    @Mock private ReserveBikeUseCase reserveBikeUseCase;
    @Mock private StartTripUseCase startTripUseCase;
    @Mock private EndTripAndBillUseCase endTripAndBillUseCase;
    @Mock private MoveBikeUseCase moveBikeUseCase;
    @Mock private SetStationStatusUseCase setStationStatusUseCase;
    @Mock private AdjustStationCapacityUseCase adjustStationCapacityUseCase;
    @Mock private ListStationSummariesUseCase listStationSummariesUseCase;
    @Mock private UserRepository userRepository;
    @Mock private JpaStationRepository stationRepository;
    @Mock private JpaBikeRepository bikeRepository;
    @Mock private TripRepository tripRepository;
    @Mock private ReservationRepository reservationRepository;
    @Mock private JpaLedgerEntryRepository ledgerEntryRepository;

    private BmsFacade bmsFacade;

    @BeforeEach
    void setUp() {
        bmsFacade = new BmsFacade(
                reserveBikeUseCase,
                startTripUseCase,
                endTripAndBillUseCase,
                moveBikeUseCase,
                setStationStatusUseCase,
                adjustStationCapacityUseCase,
                listStationSummariesUseCase,
                userRepository,
                stationRepository,
                bikeRepository,
                tripRepository,
                reservationRepository,
                ledgerEntryRepository
        );
    }
    private Rider buildRider() {
        Rider r = new Rider();
        r.setUserId(UUID.randomUUID());
        r.setRole("RIDER");
        return r;
    }
    private Station buildStationWithOneBike() {
        Bike bike = new Bike();
        bike.setId(UUID.randomUUID());
        bike.setStatus(Bike.BikeStatus.AVAILABLE);

        Station station = new Station();
        station.setId(UUID.randomUUID());
        station.setName("Test Station");
        station.markActive();
        station.addEmptyDocks(2);
        station.getDocks().get(0).setOccupiedBike(bike);
        station.updateBikesDocked();
        return station;
    }

    @Test
    void guestSeesCanReserveAndStartWhenStationHasAvailableBike() {
        Station station = buildStationWithOneBike();
        when(stationRepository.findById(station.getId())).thenReturn(station);
        StationDetailsDto details = bmsFacade.getStationDetails(station.getId(), null);
        assertThat(details.canReserve()).isTrue();
        assertThat(details.canStartTrip()).isTrue();
        assertThat(details.canReturn()).isFalse();
    }

    @Test
    void riderWithActiveTripCannotReserveOrStartButCanReturnIfFreeDock() {
        Rider rider = buildRider();
        Station station = buildStationWithOneBike();
        when(stationRepository.findById(station.getId())).thenReturn(station);
        when(userRepository.findById(rider.getUserId())).thenReturn(rider);
        when(tripRepository.riderHasActiveTrip(rider.getUserId())).thenReturn(true);
        when(reservationRepository.findByRiderId(rider.getUserId())).thenReturn(null);
        StationDetailsDto details = bmsFacade.getStationDetails(station.getId(), rider.getUserId());
        assertThat(details.canReserve()).isFalse();
        assertThat(details.canStartTrip()).isFalse();
        assertThat(details.canReturn()).isTrue();
    }

    @Test
    void riderWithReservationAtStationCanStartTripAtThatStationButCannotReserve() {
        Rider rider = buildRider();
        Station station = buildStationWithOneBike();
        Bike reservedBike = new Bike();
        reservedBike.setId(UUID.randomUUID());
        reservedBike.setStatus(Bike.BikeStatus.RESERVED);
        Reservation reservation = new Reservation (
            UUID.randomUUID(),
            rider,
            station,
            reservedBike,
            Instant.now(),
            Instant.now().plus(5, ChronoUnit.MINUTES),
            5,
            true
        );
        when(stationRepository.findById(station.getId())).thenReturn(station);
        when(userRepository.findById(rider.getUserId())).thenReturn(rider);
        when(tripRepository.riderHasActiveTrip(rider.getUserId())).thenReturn(false);
        when(reservationRepository.findByRiderId(rider.getUserId())).thenReturn(reservation);
        StationDetailsDto details = bmsFacade.getStationDetails(station.getId(), rider.getUserId());
        assertThat(details.canReserve()).isFalse();
        assertThat(details.canStartTrip()).isTrue();
        assertThat(details.canReturn()).isFalse();
    }

    @Test
    void riderWithReservationAtOtherStationCannotStartOrReserveHere() {
        Rider rider = buildRider();
        Station station = buildStationWithOneBike();
        Station other = new Station();
        other.setId(UUID.randomUUID());
        other.setName("Other");
        other.markActive();
        other.addEmptyDocks(1);
        Bike reservedBike = new Bike();
        reservedBike.setId(UUID.randomUUID());
        reservedBike.setStatus(Bike.BikeStatus.RESERVED);
        Reservation reservation = new Reservation (
            UUID.randomUUID(),
            rider,
            other,
            reservedBike,
            Instant.now(),
            Instant.now().plus(5, ChronoUnit.MINUTES),
            5,
            true
        );
        when(stationRepository.findById(station.getId())).thenReturn(station);
        when(userRepository.findById(rider.getUserId())).thenReturn(rider);
        when(tripRepository.riderHasActiveTrip(rider.getUserId())).thenReturn(false);
        when(reservationRepository.findByRiderId(rider.getUserId())).thenReturn(reservation);
        StationDetailsDto details = bmsFacade.getStationDetails(station.getId(), rider.getUserId());
        assertThat(details.canReserve()).isFalse();
        assertThat(details.canStartTrip()).isFalse();
        assertThat(details.canReturn()).isFalse();
    }

    @Test
    void stationDetailsExposeBikeTypePerDock() {
        Station station = buildStationWithOneBike();
        station.getDocks().get(0).getOccupiedBike().setType(Bike.BikeType.E_BIKE);
        when(stationRepository.findById(station.getId())).thenReturn(station);

        StationDetailsDto details = bmsFacade.getStationDetails(station.getId(), null);

        assertThat(details.docks()).isNotEmpty();
        assertThat(details.docks().get(0).bikeType()).isEqualTo(Bike.BikeType.E_BIKE);
    }
    
}
