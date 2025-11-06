package com.sharecycle.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sharecycle.domain.MonthlySubscriberStrategy;
import com.sharecycle.domain.PayAsYouGoStrategy;
import com.sharecycle.domain.TripBuilder;
import com.sharecycle.domain.event.BillIssuedEvent;
import com.sharecycle.domain.event.DomainEventPublisher;
import com.sharecycle.domain.event.StationStatusChangedEvent;
import com.sharecycle.domain.event.TripEndedEvent;
import com.sharecycle.domain.model.Bike;
import com.sharecycle.domain.model.Bill;
import com.sharecycle.domain.model.Dock;
import com.sharecycle.domain.model.LedgerEntry;
import com.sharecycle.domain.model.PricingPlan;
import com.sharecycle.domain.model.Reservation;
import com.sharecycle.domain.model.Rider;
import com.sharecycle.domain.model.Station;
import com.sharecycle.domain.model.Trip;
import com.sharecycle.domain.repository.JpaBikeRepository;
import com.sharecycle.domain.repository.JpaDockRepository;
import com.sharecycle.domain.repository.JpaLedgerEntryRepository;
import com.sharecycle.domain.repository.JpaStationRepository;
import com.sharecycle.domain.repository.PricingStrategyRepository;
import com.sharecycle.domain.repository.ReservationRepository;
import com.sharecycle.domain.repository.TripRepository;

import jakarta.transaction.Transactional;

@Service
public class EndTripAndBillUseCase {

    private static final Logger logger = LoggerFactory.getLogger(EndTripAndBillUseCase.class);

    private final DomainEventPublisher eventPublisher;
    private final TripRepository tripRepository;
    private final JpaLedgerEntryRepository ledgerEntryRepository;
    private final JpaStationRepository stationRepository;
    private final JpaDockRepository dockRepository;
    private final JpaBikeRepository bikeRepository;
    private final ReservationRepository reservationRepository;
    
    private final PayAsYouGoStrategy payAsYouGoStrategy;
    private final MonthlySubscriberStrategy monthlySubscriberStrategy;

    @Autowired
    public EndTripAndBillUseCase(DomainEventPublisher eventPublisher,
                                 TripRepository tripRepository,
                                 JpaLedgerEntryRepository ledgerEntryRepository,
                                 JpaStationRepository stationRepository,
                                 JpaDockRepository dockRepository,
                                 JpaBikeRepository bikeRepository,
                                 ReservationRepository reservationRepository) {
        this.eventPublisher = eventPublisher;
        this.tripRepository = tripRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.stationRepository = stationRepository;
        this.dockRepository = dockRepository;
        this.bikeRepository = bikeRepository;
        this.reservationRepository = reservationRepository;
        this.payAsYouGoStrategy = new PayAsYouGoStrategy();
        this.monthlySubscriberStrategy = new MonthlySubscriberStrategy();
    }

    // Test-friendly constructor (no dock repo operations)
    public EndTripAndBillUseCase(DomainEventPublisher eventPublisher,
                                 TripRepository tripRepository,
                                 JpaLedgerEntryRepository ledgerEntryRepository,
                                 JpaStationRepository stationRepository,
                                 JpaBikeRepository bikeRepository,
                                 ReservationRepository reservationRepository) {
        this(eventPublisher, tripRepository, ledgerEntryRepository, stationRepository,
                new JpaDockRepository() {
                    @Override public void save(Dock dock) { }
                    @Override public Dock findById(UUID id) { return null; }
                    @Override public List<Dock> findAll() { return List.of(); }
                    @Override public int clearBikeFromAllDocks(UUID bikeId) { return 0; }
                },
                bikeRepository, reservationRepository);
    }

    @Transactional
    public LedgerEntry execute(Trip currentTrip, Station endStation) {
        Trip managedTrip = tripRepository.findById(currentTrip.getTripID());
        if (managedTrip == null) {
            throw new IllegalArgumentException("Trip not found.");
        }
        if (managedTrip.getEndTime() != null) {
            throw new IllegalStateException("Trip already completed.");
        }

        Station managedEndStation = stationRepository.findByIdForUpdate(endStation.getId());
        if (managedEndStation == null) {
            throw new IllegalArgumentException("Destination station not found.");
        }

        Bike tripBike = managedTrip.getBike();
        if (tripBike == null) {
            throw new IllegalStateException("Bike not found for trip.");
        }

        validate(managedTrip, managedEndStation);

        // Reconcile bike status if needed to ensure trip can end cleanly
        if (tripBike.getStatus() != Bike.BikeStatus.ON_TRIP) {
            logger.warn("Bike status {} inconsistent with active trip; reconciling to ON_TRIP", tripBike.getStatus());
            tripBike.setStatus(Bike.BikeStatus.ON_TRIP);
        }

        LocalDateTime endTime = LocalDateTime.now();

        // Defensive: clear any remaining dock assignment for this bike across the system
        dockRepository.clearBikeFromAllDocks(tripBike.getId());

        // Update the station and dock
        managedEndStation.dockBike(tripBike);
        stationRepository.save(managedEndStation);
        eventPublisher.publish(new StationStatusChangedEvent(
                managedEndStation.getId(),
                managedEndStation.getStatus(),
                managedEndStation.getCapacity(),
                managedEndStation.getBikesDocked()
        ));

        // Update the bike's status now that it is docked
        tripBike.completeTrip();
        tripBike.setStatus(Bike.BikeStatus.AVAILABLE);
        tripBike.setReservationExpiry(null);
        tripBike.setCurrentStation(managedEndStation);
        bikeRepository.save(tripBike);

        // Clear any active reservation for rider
        Reservation activeReservation = reservationRepository.findByRiderId(managedTrip.getRider().getUserId());
        if (activeReservation != null) {
            activeReservation.expire();
            reservationRepository.save(activeReservation);
        }

        // End the trip and persist
        TripBuilder tripBuilder = new TripBuilder(managedTrip);
        tripBuilder.endAt(managedEndStation, endTime);
        Trip editedTrip = tripBuilder.build();
        tripRepository.save(editedTrip);
        eventPublisher.publish(new TripEndedEvent(editedTrip.getTripID()));

        // SELECT PRICING STRATEGY based on rider plan
        PricingStrategyRepository strategy = selectStrategy(editedTrip.getRider());
        String planName = resolvePlanName(editedTrip.getRider());
        
        // Create a pricing plan for the calculation
        PricingPlan pricingPlan = createPricingPlan(planName);
        
        // Calculate bill using strategy
        Bill bill = strategy.calculate(editedTrip, pricingPlan);

        // Create and persist ledger entry
        LedgerEntry ledgerEntry = new LedgerEntry(editedTrip.getRider(), editedTrip, bill, planName);
        ledgerEntryRepository.save(ledgerEntry);

        // Publish BillIssued for UI/history
        eventPublisher.publish(new BillIssuedEvent(
                editedTrip.getTripID(),
                editedTrip.getRider().getUserId(),
                bill.getBillId(),
                ledgerEntry.getLedgerId(),
                bill.getComputedAt(),
                bill.getBaseCost(),
                bill.getTimeCost(),
                bill.getEBikeSurcharge(),
                bill.getTotalCost(),
                planName
        ));

        return ledgerEntry;
    }

    private void validate(Trip currentTrip, Station station) {
        if (station.isOutOfService()) {
            logger.error("Destination station is out of service");
            throw new IllegalStateException("Destination station is out of service");
        }
        if (!station.hasFreeDock()){
            logger.error("Unexpected error: Station is full");
            throw new IllegalStateException("Unexpected error: Station is full");
        }
    }

    private PricingStrategyRepository selectStrategy(Rider rider) {
        // TODO: get actual plan from rider when subscription system is implemented
        String planType = resolvePlanName(rider);
        if ("MONTHLY_SUBSCRIBER".equalsIgnoreCase(planType)) {
            return monthlySubscriberStrategy;
        }
        return payAsYouGoStrategy;
    }

    private String resolvePlanName(Rider rider) {
        // TODO: integrate with rider's subscription info when available
        return "PAY_AS_YOU_GO";
    }

    private PricingPlan createPricingPlan(String planName) {
        if ("MONTHLY_SUBSCRIBER".equalsIgnoreCase(planName)) {
            return new PricingPlan(
                UUID.randomUUID(),
                "Monthly Subscriber",
                20.0,
                PricingPlan.PlanType.MONTHLY_SUBSCRIBER
            );
        }
        return new PricingPlan(
            UUID.randomUUID(),
            "Pay As You Go",
            0.0,
            PricingPlan.PlanType.PAY_AS_YOU_GO
        );
    }
}
