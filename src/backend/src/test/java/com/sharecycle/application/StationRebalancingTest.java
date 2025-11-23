package com.sharecycle.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sharecycle.domain.event.DomainEvent;
import com.sharecycle.domain.event.DomainEventPublisher;
import com.sharecycle.domain.event.RebalanceAlertEvent;
import com.sharecycle.domain.model.Bike;
import com.sharecycle.domain.model.Dock;
import com.sharecycle.domain.model.PricingPlan;
import com.sharecycle.domain.model.Rider;
import com.sharecycle.domain.model.Station;
import com.sharecycle.domain.model.User;
import com.sharecycle.domain.repository.JpaBikeRepository;
import com.sharecycle.domain.repository.JpaStationRepository;
import com.sharecycle.domain.repository.ReservationRepository;
import com.sharecycle.domain.repository.TripRepository;
import com.sharecycle.domain.repository.UserRepository;

/**
 * Test suite for station rebalancing alerts.
 * Verifies that RebalanceAlertEvent is triggered when a station becomes empty
 * and operators are properly notified for bike redistribution.
 */
@DisplayName("Station Rebalancing Alert Tests")
class StationRebalancingTest {

    @Mock private JpaBikeRepository bikeRepository;
    @Mock private JpaStationRepository stationRepository;
    @Mock private UserRepository userRepository;
    @Mock private TripRepository tripRepository;
    @Mock private ReservationRepository reservationRepository;
    @Mock private DomainEventPublisher eventPublisher;

    @Captor private ArgumentCaptor<DomainEvent> eventCaptor;

    private StartTripUseCase startTripUseCase;

    private UUID stationId;
    private UUID bikeId;
    private UUID riderId;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        startTripUseCase = new StartTripUseCase(
            bikeRepository,
            userRepository,
            stationRepository,
            tripRepository,
            reservationRepository,
            eventPublisher
        );

        stationId = UUID.randomUUID();
        bikeId = UUID.randomUUID();
        riderId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should trigger RebalanceAlertEvent when station becomes empty after bike checkout")
    void shouldTriggerRebalanceAlertWhenStationBecomesEmpty() {
        // Given: A station with exactly 1 available bike
        Station station = createStationWithOneBike();
        Bike bike = createAvailableBike();
        User rider = createRider();

        when(stationRepository.findByIdForUpdate(stationId)).thenReturn(station);
        when(bikeRepository.findById(bikeId)).thenReturn(bike);
        when(tripRepository.riderHasActiveTrip(riderId)).thenReturn(false);
        when(reservationRepository.findByRiderId(riderId)).thenReturn(null);
        when(reservationRepository.hasActiveReservationForBike(bikeId)).thenReturn(false);

        // When: The last bike is checked out
        startTripUseCase.execute(
            UUID.randomUUID(),
            LocalDateTime.now(),
            30,
            rider,
            bike,
            station
        );

        // Then: RebalanceAlertEvent should be published
        verify(eventPublisher, atLeastOnce()).publish(eventCaptor.capture());
        
        boolean rebalanceEventPublished = eventCaptor.getAllValues().stream()
            .anyMatch(event -> event instanceof RebalanceAlertEvent);
        
        assertThat(rebalanceEventPublished)
            .as("RebalanceAlertEvent should be published when station becomes empty")
            .isTrue();
    }

    @Test
    @DisplayName("Should include correct station details in RebalanceAlertEvent")
    void shouldIncludeCorrectStationDetailsInRebalanceAlert() {
        // Given: A station with specific details
        Station station = createStationWithOneBike();
        station.setName("Downtown Station");
        station.setAddress("123 Main St");
        
        Bike bike = createAvailableBike();
        User rider = createRider();

        when(stationRepository.findByIdForUpdate(stationId)).thenReturn(station);
        when(bikeRepository.findById(bikeId)).thenReturn(bike);
        when(tripRepository.riderHasActiveTrip(riderId)).thenReturn(false);
        when(reservationRepository.findByRiderId(riderId)).thenReturn(null);
        when(reservationRepository.hasActiveReservationForBike(bikeId)).thenReturn(false);

        // When: The bike is checked out
        startTripUseCase.execute(
            UUID.randomUUID(),
            LocalDateTime.now(),
            30,
            rider,
            bike,
            station
        );

        // Then: Event should contain correct station information
        verify(eventPublisher, atLeastOnce()).publish(eventCaptor.capture());
        
        RebalanceAlertEvent rebalanceEvent = eventCaptor.getAllValues().stream()
            .filter(event -> event instanceof RebalanceAlertEvent)
            .map(event -> (RebalanceAlertEvent) event)
            .findFirst()
            .orElse(null);

        assertThat(rebalanceEvent).isNotNull();
        assertThat(rebalanceEvent.stationId()).isEqualTo(stationId);
        assertThat(rebalanceEvent.stationName()).isEqualTo("Downtown Station");
        assertThat(rebalanceEvent.address()).isEqualTo("123 Main St");
        assertThat(rebalanceEvent.latitude()).isEqualTo(40.7128);
        assertThat(rebalanceEvent.longitude()).isEqualTo(-74.0060);
        assertThat(rebalanceEvent.capacity()).isEqualTo(10);
    }

    @Test
    @DisplayName("Should NOT trigger RebalanceAlertEvent when station still has available bikes")
    void shouldNotTriggerRebalanceAlertWhenStationHasBikesRemaining() {
        // Given: A station with 2 available bikes
        Station station = createStationWithMultipleBikes(2);
        Bike bike = createAvailableBike();
        User rider = createRider();

        when(stationRepository.findByIdForUpdate(stationId)).thenReturn(station);
        when(bikeRepository.findById(bikeId)).thenReturn(bike);
        when(tripRepository.riderHasActiveTrip(riderId)).thenReturn(false);
        when(reservationRepository.findByRiderId(riderId)).thenReturn(null);
        when(reservationRepository.hasActiveReservationForBike(bikeId)).thenReturn(false);

        // When: One bike is checked out (1 remains)
        startTripUseCase.execute(
            UUID.randomUUID(),
            LocalDateTime.now(),
            30,
            rider,
            bike,
            station
        );

        // Then: RebalanceAlertEvent should NOT be published
        verify(eventPublisher, atLeastOnce()).publish(eventCaptor.capture());
        
        boolean rebalanceEventPublished = eventCaptor.getAllValues().stream()
            .anyMatch(event -> event instanceof RebalanceAlertEvent);
        
        assertThat(rebalanceEventPublished)
            .as("RebalanceAlertEvent should NOT be published when bikes remain")
            .isFalse();
    }

    @Test
    @DisplayName("Should NOT trigger RebalanceAlertEvent when station is out of service")
    void shouldNotTriggerRebalanceAlertWhenStationOutOfService() {
        // Given: An out-of-service station (even if it becomes empty, no alert needed)
        Station station = createStationWithOneBike();
        station.markOutOfService();
        
        Bike bike = createAvailableBike();
        User rider = createRider();

        when(stationRepository.findByIdForUpdate(stationId)).thenReturn(station);
        when(bikeRepository.findById(bikeId)).thenReturn(bike);
        when(tripRepository.riderHasActiveTrip(riderId)).thenReturn(false);
        when(reservationRepository.findByRiderId(riderId)).thenReturn(null);
        when(reservationRepository.hasActiveReservationForBike(bikeId)).thenReturn(false);

        // When: The bike is checked out from out-of-service station
        // Note: This would normally throw an exception, but we're testing the event logic
        try {
            startTripUseCase.execute(
                UUID.randomUUID(),
                LocalDateTime.now(),
                30,
                rider,
                bike,
                station
            );
        } catch (IllegalStateException e) {
            // Expected - station is out of service
        }

        // Then: RebalanceAlertEvent should NOT be published (even if it reached that code)
        verify(eventPublisher, never()).publish(argThat(event -> event instanceof RebalanceAlertEvent));
    }

    @Test
    @DisplayName("Should emit RebalanceAlertEvent without errors")
    void shouldEmitRebalanceAlertWithoutErrors() {
        // Given: A station with one bike
        Station station = createStationWithOneBike();
        Bike bike = createAvailableBike();
        User rider = createRider();

        when(stationRepository.findByIdForUpdate(stationId)).thenReturn(station);
        when(bikeRepository.findById(bikeId)).thenReturn(bike);
        when(tripRepository.riderHasActiveTrip(riderId)).thenReturn(false);
        when(reservationRepository.findByRiderId(riderId)).thenReturn(null);
        when(reservationRepository.hasActiveReservationForBike(bikeId)).thenReturn(false);

        // When/Then: No exceptions should be thrown during event emission
        assertThatCode(() -> startTripUseCase.execute(
            UUID.randomUUID(),
            LocalDateTime.now(),
            30,
            rider,
            bike,
            station
        )).doesNotThrowAnyException();

        // Verify event was published
        verify(eventPublisher, atLeastOnce()).publish(any(RebalanceAlertEvent.class));
    }

    @Test
    @DisplayName("Should trigger RebalanceAlertEvent consistently on repeated empty conditions")
    void shouldTriggerRebalanceAlertConsistently() {
        // Test that the alert triggers reliably every time a station becomes empty
        for (int i = 0; i < 5; i++) {
            // Reset mocks for each iteration
            reset(eventPublisher, stationRepository, bikeRepository, tripRepository, reservationRepository);

            Station station = createStationWithOneBike();
            Bike bike = createAvailableBike();
            User rider = createRider();

            when(stationRepository.findByIdForUpdate(stationId)).thenReturn(station);
            when(bikeRepository.findById(bikeId)).thenReturn(bike);
            when(tripRepository.riderHasActiveTrip(riderId)).thenReturn(false);
            when(reservationRepository.findByRiderId(riderId)).thenReturn(null);
            when(reservationRepository.hasActiveReservationForBike(bikeId)).thenReturn(false);

            startTripUseCase.execute(
                UUID.randomUUID(),
                LocalDateTime.now(),
                30,
                rider,
                bike,
                station
            );

            // Verify event published each time
            verify(eventPublisher, atLeastOnce()).publish(argThat(event -> event instanceof RebalanceAlertEvent));
        }
    }

    @Test
    @DisplayName("RebalanceAlertEvent should have valid timestamp")
    void rebalanceAlertEventShouldHaveValidTimestamp() {
        // Given: A station with one bike
        Station station = createStationWithOneBike();
        Bike bike = createAvailableBike();
        User rider = createRider();

        when(stationRepository.findByIdForUpdate(stationId)).thenReturn(station);
        when(bikeRepository.findById(bikeId)).thenReturn(bike);
        when(tripRepository.riderHasActiveTrip(riderId)).thenReturn(false);
        when(reservationRepository.findByRiderId(riderId)).thenReturn(null);
        when(reservationRepository.hasActiveReservationForBike(bikeId)).thenReturn(false);

        LocalDateTime beforeAction = LocalDateTime.now().minusSeconds(1);

        // When: Bike is checked out
        startTripUseCase.execute(
            UUID.randomUUID(),
            LocalDateTime.now(),
            30,
            rider,
            bike,
            station
        );

        LocalDateTime afterAction = LocalDateTime.now().plusSeconds(1);

        // Then: Event timestamp should be within valid range
        verify(eventPublisher, atLeastOnce()).publish(eventCaptor.capture());
        
        RebalanceAlertEvent rebalanceEvent = eventCaptor.getAllValues().stream()
            .filter(event -> event instanceof RebalanceAlertEvent)
            .map(event -> (RebalanceAlertEvent) event)
            .findFirst()
            .orElse(null);

        assertThat(rebalanceEvent).isNotNull();
        assertThat(rebalanceEvent.occurredAt())
            .isAfterOrEqualTo(beforeAction)
            .isBeforeOrEqualTo(afterAction);
    }

    // ====== Helper Methods ======

    private Station createStationWithOneBike() {
        Station station = new Station();
        station.setId(stationId);
        station.setName("Test Station");
        station.setLatitude(40.7128);
        station.setLongitude(-74.0060);
        station.setAddress("123 Main St");

        // Create docks list
        java.util.List<Dock> docksList = new java.util.ArrayList<>();
        
        // Add 1 dock with bike
        Dock dockWithBike = new Dock();
        Bike bike = createAvailableBike();
        dockWithBike.setOccupiedBike(bike);
        docksList.add(dockWithBike);
        
        // Add 9 empty docks
        for (int i = 0; i < 9; i++) {
            Dock emptyDock = new Dock();
            docksList.add(emptyDock);
        }
        
        // Set docks - this will recalculate capacity and bikesDocked
        station.setDocks(docksList);

        return station;
    }

    private Station createStationWithMultipleBikes(int bikeCount) {
        Station station = new Station();
        station.setId(stationId);
        station.setName("Test Station");
        station.setLatitude(40.7128);
        station.setLongitude(-74.0060);
        station.setAddress("123 Main St");

        // Create docks list
        java.util.List<Dock> docksList = new java.util.ArrayList<>();
        
        // Add docks with bikes
        for (int i = 0; i < bikeCount; i++) {
            Dock dock = new Dock();
            Bike bike = new Bike();
            bike.setId(i == 0 ? bikeId : UUID.randomUUID());
            bike.setStatus(Bike.BikeStatus.AVAILABLE);
            bike.setType(Bike.BikeType.STANDARD);
            dock.setOccupiedBike(bike);
            docksList.add(dock);
        }

        // Add empty docks
        for (int i = bikeCount; i < 10; i++) {
            Dock emptyDock = new Dock();
            docksList.add(emptyDock);
        }
        
        // Set docks - this will recalculate capacity and bikesDocked
        station.setDocks(docksList);

        return station;
    }

    private Bike createAvailableBike() {
        Bike bike = new Bike();
        bike.setId(bikeId);
        bike.setStatus(Bike.BikeStatus.AVAILABLE);
        bike.setType(Bike.BikeType.STANDARD);
        return bike;
    }

    private User createRider() {
        Rider rider = new Rider(
            "Test Rider",
            "456 Oak Ave",
            "rider@test.com",
            "testrider",
            "hashedpassword",
            "tok_test123",
            PricingPlan.PlanType.PAY_AS_YOU_GO
        );
        rider.setUserId(riderId);
        return rider;
    }
}
