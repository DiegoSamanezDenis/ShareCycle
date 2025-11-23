package com.sharecycle.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sharecycle.domain.TripBuilder;
import com.sharecycle.domain.model.Bike;
import com.sharecycle.domain.model.LedgerEntry;
import com.sharecycle.domain.model.Rider;
import com.sharecycle.domain.model.Station;
import com.sharecycle.domain.model.Trip;
import com.sharecycle.domain.repository.JpaBikeRepository;
import com.sharecycle.domain.repository.JpaLedgerEntryRepository;
import com.sharecycle.domain.repository.JpaStationRepository;
import com.sharecycle.domain.repository.ReservationRepository;
import com.sharecycle.domain.repository.TripRepository;
import com.sharecycle.domain.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class BmsFacadeEndTripTest {

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
    private Trip activeTrip;
    private Station blockedStation;

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

        blockedStation = buildStation(UUID.randomUUID(), false);

        activeTrip = buildActiveTrip(blockedStation);
    }

    @Test
    void blockedReturnIncludesNearbySuggestionsWhenAvailable() {
        UUID tripId = activeTrip.getTripID();
        UUID stationId = blockedStation.getId();
        Station alternative = buildStation(UUID.randomUUID(), true);

        when(tripRepository.findById(tripId)).thenReturn(activeTrip);
        when(stationRepository.findById(stationId)).thenReturn(blockedStation);
        when(endTripAndBillUseCase.execute(activeTrip, blockedStation))
                .thenThrow(new StationFullException(stationId));
        when(stationRepository.findAll()).thenReturn(List.of(blockedStation, alternative));

        BmsFacade.TripCompletionResult result = bmsFacade.endTrip(tripId, stationId);

        assertThat(result.isBlocked()).isTrue();
        BmsFacade.ReturnBlockInfo info = result.blockInfo();
        assertThat(info).isNotNull();
        assertThat(info.stationId()).isEqualTo(stationId);
        assertThat(info.suggestions())
                .hasSize(1)
                .allSatisfy(suggestion -> {
                    assertThat(suggestion.stationId()).isEqualTo(alternative.getId());
                    assertThat(suggestion.freeDocks()).isGreaterThan(0);
                    assertThat(suggestion.distanceMeters()).isGreaterThan(0);
                });
        assertThat(info.hasCredit()).isFalse();

        verify(ledgerEntryRepository, never()).save(any());
    }

    @Test
    void blockedReturnIssuesCreditWhenNoAlternativesExist() {
        UUID tripId = activeTrip.getTripID();
        UUID stationId = blockedStation.getId();

        when(tripRepository.findById(tripId)).thenReturn(activeTrip);
        when(stationRepository.findById(stationId)).thenReturn(blockedStation);
        when(endTripAndBillUseCase.execute(activeTrip, blockedStation))
                .thenThrow(new StationFullException(stationId));
        when(stationRepository.findAll()).thenReturn(List.of(blockedStation));
        when(ledgerEntryRepository.findAllByUser(activeTrip.getRider())).thenReturn(List.of());

        ArgumentCaptor<LedgerEntry> creditCaptor = ArgumentCaptor.forClass(LedgerEntry.class);

        BmsFacade.TripCompletionResult result = bmsFacade.endTrip(tripId, stationId);

        assertThat(result.isBlocked()).isTrue();
        BmsFacade.ReturnBlockInfo info = result.blockInfo();
        assertThat(info).isNotNull();
        assertThat(info.hasCredit()).isTrue();
        assertThat(info.suggestions()).isEmpty();
        LedgerEntry creditEntry = info.creditLedgerEntry();
        assertThat(creditEntry).isNotNull();
        assertThat(creditEntry.getTrip()).isNull();
        assertThat(creditEntry.getUser()).isEqualTo(activeTrip.getRider());
        assertThat(creditEntry.getBill()).isNotNull();
        assertThat(Math.abs(creditEntry.getBill().getTotalCost())).isEqualTo(1.00d);
        assertThat(creditEntry.getDescription())
                .containsIgnoringCase("credit")
                .contains(activeTrip.getTripID().toString());

        verify(ledgerEntryRepository).save(creditCaptor.capture());
        LedgerEntry persistedCredit = creditCaptor.getValue();
        assertThat(persistedCredit.getLedgerId()).isEqualTo(creditEntry.getLedgerId());
    }

    @Test
    void blockedReturnReusesExistingCreditOnRetry() {
        UUID tripId = activeTrip.getTripID();
        UUID stationId = blockedStation.getId();

        when(tripRepository.findById(tripId)).thenReturn(activeTrip);
        when(stationRepository.findById(stationId)).thenReturn(blockedStation);
        when(endTripAndBillUseCase.execute(activeTrip, blockedStation))
                .thenThrow(new StationFullException(stationId));
        when(stationRepository.findAll()).thenReturn(List.of(blockedStation));

        AtomicReference<LedgerEntry> savedCredit = new AtomicReference<>();
        when(ledgerEntryRepository.findAllByUser(activeTrip.getRider())).thenAnswer(invocation -> {
            LedgerEntry entry = savedCredit.get();
            return entry == null ? List.of() : List.of(entry);
        });
        doAnswer(invocation -> {
            LedgerEntry ledgerEntry = invocation.getArgument(0);
            savedCredit.set(ledgerEntry);
            return null;
        }).when(ledgerEntryRepository).save(any());

        BmsFacade.TripCompletionResult firstResult = bmsFacade.endTrip(tripId, stationId);
        LedgerEntry firstCredit = firstResult.blockInfo().creditLedgerEntry();
        assertThat(firstCredit).isNotNull();
        verify(ledgerEntryRepository, times(1)).save(any());

        BmsFacade.TripCompletionResult secondResult = bmsFacade.endTrip(tripId, stationId);
        LedgerEntry reusedCredit = secondResult.blockInfo().creditLedgerEntry();
        assertThat(reusedCredit).isNotNull();
        assertThat(reusedCredit.getLedgerId()).isEqualTo(firstCredit.getLedgerId());
        assertThat(secondResult.blockInfo().message()).contains("already applied");

        verify(ledgerEntryRepository, times(1)).save(any());
    }

    private Trip buildActiveTrip(Station startStation) {
        Rider rider = new Rider();
        rider.setUserId(UUID.randomUUID());
        rider.setRole("RIDER");
        Bike bike = new Bike();
        bike.setId(UUID.randomUUID());
        bike.setStatus(Bike.BikeStatus.ON_TRIP);
        TripBuilder builder = new TripBuilder();
        builder.setTripId(UUID.randomUUID());
        builder.start(rider, startStation, bike, LocalDateTime.now());
        return builder.build();
    }

    private Station buildStation(UUID id, boolean leaveDockFree) {
        Station station = new Station();
        station.setId(id);
        station.setName("Station " + id.toString().substring(0, 8));
        double offset = leaveDockFree ? 0.02 : 0.01;
        station.setLatitude(45.0 + offset);
        station.setLongitude(-73.0 + offset);
        station.markActive();
        station.addEmptyDocks(2);
        if (!leaveDockFree) {
            fillStation(station);
        }
        return station;
    }

    private void fillStation(Station station) {
        station.getDocks().forEach(dock -> {
            Bike dockedBike = new Bike();
            dockedBike.setId(UUID.randomUUID());
            dockedBike.setStatus(Bike.BikeStatus.AVAILABLE);
            dock.setOccupiedBike(dockedBike);
        });
        station.updateBikesDocked();
    }
}
