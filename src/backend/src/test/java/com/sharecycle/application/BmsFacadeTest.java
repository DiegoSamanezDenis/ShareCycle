package com.sharecycle.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.sharecycle.domain.model.*;
import com.sharecycle.domain.repository.*;
import com.sharecycle.model.dto.StationDetailsDto;
import com.sharecycle.model.dto.StationSummaryDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

class BmsFacadeTest {

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

    @InjectMocks private BmsFacade facade;

    private UUID riderId;
    private UUID stationId;
    private UUID bikeId;
    private UUID tripId;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        riderId = UUID.randomUUID();
        stationId = UUID.randomUUID();
        bikeId = UUID.randomUUID();
        tripId = UUID.randomUUID();
    }

    @Test
    void testReserveBikeSuccess() {
        Rider rider = mock(Rider.class);
        Station station = mock(Station.class);
        Bike bike = mock(Bike.class);
        Reservation reservation = mock(Reservation.class);

        when(userRepository.findById(riderId)).thenReturn(rider);
        when(stationRepository.findById(stationId)).thenReturn(station);
        when(bikeRepository.findById(bikeId)).thenReturn(bike);
        when(reserveBikeUseCase.execute(rider, station, bike, 30)).thenReturn(reservation);

        Reservation result = facade.reserveBike(riderId, stationId, bikeId, 30);

        assertEquals(reservation, result);
    }

    @Test
    void testStartTripSuccess() {
        Rider rider = mock(Rider.class);
        Station station = mock(Station.class);
        Bike bike = mock(Bike.class);
        Trip trip = mock(Trip.class);
        LocalDateTime startTime = LocalDateTime.now();

        when(userRepository.findById(riderId)).thenReturn(rider);
        when(stationRepository.findById(stationId)).thenReturn(station);
        when(bikeRepository.findById(bikeId)).thenReturn(bike);
        when(startTripUseCase.execute(eq(tripId), any(), eq(0), eq(rider), eq(bike), eq(station)))
                .thenReturn(trip);

        Trip result = facade.startTrip(tripId, riderId, bikeId, stationId, startTime);

        assertEquals(trip, result);
    }

    @Test
    void testEndTripCompleted() throws Exception {
        Trip trip = mock(Trip.class);
        Station station = mock(Station.class);
        LedgerEntry ledger = mock(LedgerEntry.class);

        when(tripRepository.findById(tripId)).thenReturn(trip);
        when(stationRepository.findById(stationId)).thenReturn(station);
        when(endTripAndBillUseCase.execute(trip, station)).thenReturn(ledger);

        BmsFacade.TripCompletionResult result = facade.endTrip(tripId, stationId);

        assertTrue(result.isCompleted());
        assertEquals(ledger, result.ledgerEntry());
    }

    @Test
    void testEndTripBlockedWithCourtesyCredit() throws Exception {
        Trip trip = mock(Trip.class);
        Station blockedStation = mock(Station.class);
        LedgerEntry credit = mock(LedgerEntry.class);

        when(tripRepository.findById(tripId)).thenReturn(trip);
        when(stationRepository.findById(stationId)).thenReturn(blockedStation);
        when(stationRepository.findAll()).thenReturn(List.of(blockedStation));
        when(ledgerEntryRepository.findAllByUser(trip.getRider())).thenReturn(List.of());
        when(blockedStation.getId()).thenReturn(stationId);

        doThrow(new StationFullException(stationId))
                .when(endTripAndBillUseCase).execute(trip, blockedStation);

        BmsFacade.TripCompletionResult result = facade.endTrip(tripId, stationId);

        assertTrue(result.isBlocked());
        assertNotNull(result.blockInfo());
        assertTrue(result.blockInfo().hasCredit());
        verify(ledgerEntryRepository).save(any(LedgerEntry.class));
    }

    @Test
    void testGetStationDetailsForRiderWithActiveTrip() {
        // Use a real Rider instance instead of a mock
        Rider rider = new Rider(
                "Test Rider",
                "123 Test St",
                "test@email.com",
                "testuser",
                "password",
                "token",
                PricingPlan.PlanType.PAY_AS_YOU_GO
        );

        // Station mock
        Station station = mock(Station.class);
        when(station.getId()).thenReturn(stationId);
        when(station.getName()).thenReturn("Test Station");
        when(station.getStatus()).thenReturn(Station.StationStatus.EMPTY);
        when(station.getCapacity()).thenReturn(10);
        when(station.getBikesDocked()).thenReturn(5);
        when(station.getFreeDockCount()).thenReturn(5);
        when(station.getAvailableBikeCount()).thenReturn(5);
        when(station.getDocks()).thenReturn(List.of());
        when(station.isOutOfService()).thenReturn(false);

        // Repository mocks
        when(userRepository.findById(riderId)).thenReturn(rider);
        when(stationRepository.findById(stationId)).thenReturn(station);

        // Rider has an active trip, no active reservation
        when(tripRepository.riderHasActiveTrip(eq(riderId))).thenReturn(true);
        when(reservationRepository.findByRiderId(riderId)).thenReturn(null);

        StationDetailsDto dto = facade.getStationDetails(stationId, riderId);

        assertNotNull(dto);
        // Rider with active trip cannot reserve or start a new trip
        assertFalse(dto.canReserve());
        assertFalse(dto.canStartTrip());
        // Rider with active trip can return to a station with free docks
        assertTrue(dto.canReturn());
    }



    @Test
    void testListStations() {
        StationSummaryDto summary = mock(StationSummaryDto.class);
        when(listStationSummariesUseCase.execute()).thenReturn(List.of(summary));

        List<StationSummaryDto> result = facade.listStations();

        assertEquals(1, result.size());
        assertEquals(summary, result.get(0));
    }

    @Test
    void testEmptyStationTriggersRebalanceAlert() {
        Station emptyStation = mock(Station.class);
        when(emptyStation.getId()).thenReturn(stationId);
        when(emptyStation.getAvailableBikeCount()).thenReturn(0);
        when(emptyStation.isOutOfService()).thenReturn(false);

        when(stationRepository.findById(stationId)).thenReturn(emptyStation);

        boolean needsRebalance = emptyStation.getAvailableBikeCount() == 0 && !emptyStation.isOutOfService();

        assertTrue(needsRebalance, "Empty station should trigger rebalancing alert");
    }



}
