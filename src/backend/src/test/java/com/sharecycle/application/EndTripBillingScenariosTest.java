package com.sharecycle.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sharecycle.domain.DefaultPricingPlans;
import com.sharecycle.domain.TripBuilder;
import com.sharecycle.domain.event.BillIssuedEvent;
import com.sharecycle.domain.event.DomainEvent;
import com.sharecycle.domain.event.DomainEventPublisher;
import com.sharecycle.domain.event.FlexCreditAddedEvent;
import com.sharecycle.domain.model.Bike;
import com.sharecycle.domain.model.Bike.BikeStatus;
import com.sharecycle.domain.model.Bike.BikeType;
import com.sharecycle.domain.model.Bill;
import com.sharecycle.domain.model.Dock;
import com.sharecycle.domain.model.LedgerEntry;
import com.sharecycle.domain.model.LedgerEntry.LedgerStatus;
import com.sharecycle.domain.model.LoyaltyTier;
import com.sharecycle.domain.model.PricingPlan;
import com.sharecycle.domain.model.Rider;
import com.sharecycle.domain.model.Station;
import com.sharecycle.domain.model.Trip;
import com.sharecycle.domain.repository.JpaBikeRepository;
import com.sharecycle.domain.repository.JpaLedgerEntryRepository;
import com.sharecycle.domain.repository.JpaLoyaltyRepository;
import com.sharecycle.domain.repository.JpaStationRepository;
import com.sharecycle.domain.repository.JpaLoyaltyRepository;
import com.sharecycle.domain.repository.ReservationRepository;
import com.sharecycle.domain.repository.TripRepository;
import com.sharecycle.infrastructure.persistence.JpaUserRepository;

@ExtendWith(MockitoExtension.class)
class EndTripBillingScenariosTest {

    @Mock
    private DomainEventPublisher eventPublisher;
    @Mock
    private TripRepository tripRepository;
    @Mock
    private JpaLedgerEntryRepository ledgerEntryRepository;
    @Mock
    private JpaStationRepository stationRepository;
    @Mock
    private JpaBikeRepository bikeRepository;
    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private JpaUserRepository userRepository;
    @Mock
    private JpaLoyaltyRepository loyaltyRepository;

    private EndTripAndBillUseCase useCase;

    @Captor
    private ArgumentCaptor<LedgerEntry> ledgerEntryCaptor;
    @Captor
    private ArgumentCaptor<Trip> tripCaptor;
    @Captor
    private ArgumentCaptor<DomainEvent> eventCaptor;
    @Captor
    private ArgumentCaptor<Bike> bikeCaptor;

    private static final PricingPlan PAYG_PLAN = DefaultPricingPlans.planForType(PricingPlan.PlanType.PAY_AS_YOU_GO);

    @BeforeEach
    void setUp() {
        useCase = new EndTripAndBillUseCase(
                eventPublisher,
                tripRepository,
                ledgerEntryRepository,
                stationRepository,
                bikeRepository,
                reservationRepository,
                userRepository,
                loyaltyRepository
        );
    }

    @Test
    void scenarioOne_calculatesBillAndPersistsLedgerWithLoyaltyDiscount() {
        Rider rider = createRider(PricingPlan.PlanType.PAY_AS_YOU_GO, 0.0, "RIDER");
        Bike bike = createBike(BikeType.STANDARD);
        Station startStation = stationWithOccupancy("Start Hub", 4, 2);
        Trip trip = buildActiveTrip(rider, bike, startStation, LocalDateTime.now().minusMinutes(18));
        Station destination = stationWithOccupancy("Busy Station", 4, 3);

        when(tripRepository.findById(trip.getTripID())).thenReturn(trip);
        when(stationRepository.findByIdForUpdate(destination.getId())).thenReturn(destination);
        when(reservationRepository.findByRiderId(rider.getUserId())).thenReturn(null);
        when(loyaltyRepository.findCurrentTier(rider.getUserId())).thenReturn(LoyaltyTier.BRONZE);
        when(userRepository.findById(rider.getUserId())).thenReturn(rider);

        LedgerEntry returnedLedger = useCase.execute(trip, destination);

        verify(tripRepository).save(tripCaptor.capture());
        Trip persistedTrip = tripCaptor.getValue();
        assertThat(persistedTrip.getEndTime()).isNotNull();
        assertThat(persistedTrip.getDurationMinutes()).isGreaterThanOrEqualTo(0);

        verify(ledgerEntryRepository).save(ledgerEntryCaptor.capture());
        LedgerEntry savedLedger = ledgerEntryCaptor.getValue();
        Bill bill = savedLedger.getBill();
        Trip billedTrip = savedLedger.getTrip();
        assertThat(bill).isNotNull();
        assertThat(billedTrip).isNotNull();
        assertThat(billedTrip.getAppliedDiscountRate()).isEqualTo(0.05);
        assertThat(savedLedger.getPricingPlan()).isEqualTo(PricingPlan.PlanType.PAY_AS_YOU_GO.name());
        assertThat(savedLedger.getLedgerStatus()).isEqualTo(LedgerStatus.PENDING);

        int durationMinutes = billedTrip.getDurationMinutes();
        double expectedTimeCost = durationMinutes * PAYG_PLAN.getPerMinuteRate() * (1 - 0.05);
        assertThat(bill.getBaseCost()).isZero();
        assertThat(bill.getEBikeSurcharge()).isZero();
        assertThat(bill.getTimeCost()).isCloseTo(expectedTimeCost, within(1e-6));
        assertThat(bill.getTotalCost())
                .isEqualTo(bill.getBaseCost() + bill.getTimeCost() + bill.getEBikeSurcharge() - bill.getFlexCreditApplied());
        assertThat(returnedLedger.getLedgerId()).isEqualTo(savedLedger.getLedgerId());

        verify(bikeRepository).save(bikeCaptor.capture());
        assertThat(bikeCaptor.getValue().getStatus()).isEqualTo(BikeStatus.AVAILABLE);

        verify(eventPublisher, atLeastOnce()).publish(eventCaptor.capture());
        BillIssuedEvent billIssuedEvent = eventCaptor.getAllValues().stream()
                .filter(event -> event instanceof BillIssuedEvent)
                .map(event -> (BillIssuedEvent) event)
                .findFirst()
                .orElseThrow();
        assertThat(billIssuedEvent.discountRate()).isEqualTo(0.05);
        assertThat(billIssuedEvent.totalCost()).isEqualTo(bill.getTotalCost());
        assertThat(billIssuedEvent.pricingPlan()).isEqualTo(savedLedger.getPricingPlan());

        verify(userRepository).save(rider);
    }

    @Test
    void scenarioTwo_eBikeRideEarnsFlexCreditWhenStationNeedsBikes() {
        Rider rider = createRider(PricingPlan.PlanType.PAY_AS_YOU_GO, 2.0, "RIDER");
        Bike bike = createBike(BikeType.E_BIKE);
        Station startStation = stationWithOccupancy("Start Hub", 3, 1);
        Trip trip = buildActiveTrip(rider, bike, startStation, LocalDateTime.now().minusMinutes(22));
        Station emptyStation = stationWithOccupancy("Sparse Station", 8, 0);

        when(tripRepository.findById(trip.getTripID())).thenReturn(trip);
        when(stationRepository.findByIdForUpdate(emptyStation.getId())).thenReturn(emptyStation);
        when(reservationRepository.findByRiderId(rider.getUserId())).thenReturn(null);
        when(loyaltyRepository.findCurrentTier(rider.getUserId())).thenReturn(LoyaltyTier.GOLD);
        when(userRepository.findById(rider.getUserId())).thenReturn(rider);

        LedgerEntry returnedLedger = useCase.execute(trip, emptyStation);

        verify(ledgerEntryRepository).save(ledgerEntryCaptor.capture());
        LedgerEntry savedLedger = ledgerEntryCaptor.getValue();
        Bill bill = savedLedger.getBill();
        Trip billedTrip = savedLedger.getTrip();
        assertThat(bill).isNotNull();
        assertThat(billedTrip.getAppliedDiscountRate()).isEqualTo(0.15);

        int durationMinutes = billedTrip.getDurationMinutes();
        double discountMultiplier = 1 - 0.15;
        double expectedTimeCost = durationMinutes * PAYG_PLAN.getPerMinuteRate() * discountMultiplier;
        double eBikeRate = PAYG_PLAN.getEBikeSurchargePerMinute() != null ? PAYG_PLAN.getEBikeSurchargePerMinute() : 0.0;
        double expectedSurcharge = durationMinutes * eBikeRate * discountMultiplier;
        assertThat(bill.getTimeCost()).isCloseTo(expectedTimeCost, within(1e-6));
        assertThat(bill.getEBikeSurcharge()).isCloseTo(expectedSurcharge, within(1e-6));
        assertThat(bill.getTotalCost())
                .isEqualTo(bill.getBaseCost() + bill.getTimeCost() + bill.getEBikeSurcharge() - bill.getFlexCreditApplied());

        double expectedCredit = Math.floor(bill.getTotalCost() * 0.05 * 100) / 100.0;
        assertThat(rider.getFlexCredit())
                .as("Existing credit is consumed before new credit is awarded")
                .isEqualTo(expectedCredit);
        verify(userRepository, times(2)).save(rider);

        verify(eventPublisher, atLeastOnce()).publish(eventCaptor.capture());
        FlexCreditAddedEvent creditEvent = eventCaptor.getAllValues().stream()
                .filter(event -> event instanceof FlexCreditAddedEvent)
                .map(event -> (FlexCreditAddedEvent) event)
                .findFirst()
                .orElseThrow();
        assertThat(creditEvent.amount()).isEqualTo(expectedCredit);

        BillIssuedEvent billIssuedEvent = eventCaptor.getAllValues().stream()
                .filter(event -> event instanceof BillIssuedEvent)
                .map(event -> (BillIssuedEvent) event)
                .findFirst()
                .orElseThrow();
        assertThat(billIssuedEvent.discountRate()).isEqualTo(0.15);
        assertThat(returnedLedger.getLedgerId()).isEqualTo(savedLedger.getLedgerId());
    }

    private Trip buildActiveTrip(Rider rider, Bike bike, Station startStation, LocalDateTime startTime) {
        TripBuilder builder = new TripBuilder();
        builder.setTripId(UUID.randomUUID());
        builder.start(rider, startStation, bike, startTime);
        return builder.build();
    }

    private Rider createRider(PricingPlan.PlanType planType, double flexCredit, String role) {
        Rider rider = new Rider();
        rider.setUserId(UUID.randomUUID());
        rider.setFullName("Test Rider");
        rider.setEmail("rider@example.com");
        rider.setRole(role);
        rider.setPricingPlanType(planType);
        rider.setFlexCredit(flexCredit);
        rider.touchOnCreate();
        return rider;
    }

    private Bike createBike(BikeType type) {
        Bike bike = new Bike();
        bike.setId(UUID.randomUUID());
        bike.setType(type);
        bike.setStatus(BikeStatus.ON_TRIP);
        return bike;
    }

    private Station stationWithOccupancy(String name, int capacity, int occupied) {
        Station station = new Station();
        station.setId(UUID.randomUUID());
        station.setName(name);
        station.setLatitude(45.5);
        station.setLongitude(-73.5);
        station.markActive();
        station.addEmptyDocks(capacity);
        for (int i = 0; i < occupied; i++) {
            Dock dock = station.getDocks().get(i);
            Bike dockedBike = new Bike();
            dockedBike.setId(UUID.randomUUID());
            dockedBike.setStatus(BikeStatus.AVAILABLE);
            dock.setOccupiedBike(dockedBike);
        }
        station.updateBikesDocked();
        return station;
    }
}
