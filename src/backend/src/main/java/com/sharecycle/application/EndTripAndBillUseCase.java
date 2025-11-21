package com.sharecycle.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.sharecycle.domain.event.*;
import com.sharecycle.domain.model.*;
import com.sharecycle.infrastructure.persistence.JpaUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sharecycle.domain.DefaultPricingPlans;
import com.sharecycle.domain.MonthlySubscriberStrategy;
import com.sharecycle.domain.PayAsYouGoStrategy;
import com.sharecycle.domain.TripBuilder;
import com.sharecycle.domain.repository.JpaBikeRepository;
import com.sharecycle.domain.repository.JpaDockRepository;
import com.sharecycle.domain.repository.JpaLedgerEntryRepository;
import com.sharecycle.domain.repository.JpaStationRepository;
import com.sharecycle.domain.repository.PricingStrategyRepository;
import com.sharecycle.domain.repository.ReservationRepository;
import com.sharecycle.domain.repository.TripRepository;
import com.sharecycle.domain.model.LoyaltyTier;
import com.sharecycle.domain.repository.JpaLoyaltyRepository;

import org.springframework.transaction.annotation.Transactional;

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
    private final JpaUserRepository userRepository;
    private final JpaLoyaltyRepository loyaltyRepository;
    
    private final PayAsYouGoStrategy payAsYouGoStrategy;
    private final MonthlySubscriberStrategy monthlySubscriberStrategy;

    @Autowired
    public EndTripAndBillUseCase(DomainEventPublisher eventPublisher,
                                 TripRepository tripRepository,
                                 JpaLedgerEntryRepository ledgerEntryRepository,
                                 JpaStationRepository stationRepository,
                                 JpaDockRepository dockRepository,
                                 JpaBikeRepository bikeRepository,
                                 ReservationRepository reservationRepository, 
                                 JpaUserRepository userRepository,
                                 JpaLoyaltyRepository loyaltyRepository) {
        this.eventPublisher = eventPublisher;
        this.tripRepository = tripRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.stationRepository = stationRepository;
        this.dockRepository = dockRepository;
        this.bikeRepository = bikeRepository;
        this.reservationRepository = reservationRepository;
        this.userRepository = userRepository;
        this.loyaltyRepository = loyaltyRepository;
        this.payAsYouGoStrategy = new PayAsYouGoStrategy();
        this.monthlySubscriberStrategy = new MonthlySubscriberStrategy();
    }

    // Test-friendly constructor (no dock repo operations)
    public EndTripAndBillUseCase(DomainEventPublisher eventPublisher,
                                 TripRepository tripRepository,
                                 JpaLedgerEntryRepository ledgerEntryRepository,
                                 JpaStationRepository stationRepository,
                                 JpaBikeRepository bikeRepository,
                                 ReservationRepository reservationRepository, 
                                 JpaUserRepository userRepository,
                                 JpaLoyaltyRepository loyaltyRepository) {
        this(eventPublisher, tripRepository, ledgerEntryRepository, stationRepository,
                new JpaDockRepository() {
                    @Override public void save(Dock dock) { }
                    @Override public Dock findById(UUID id) { return null; }
                    @Override public List<Dock> findAll() { return List.of(); }
                    @Override public int clearBikeFromAllDocks(UUID bikeId) { return 0; }
                },
                bikeRepository, reservationRepository, userRepository, loyaltyRepository);
    }

    @Transactional(noRollbackFor = StationFullException.class)
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
        try {
            managedEndStation.dockBike(tripBike);
        } catch (IllegalStateException ex) {
            if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("no free dock")) {
                throw new StationFullException(managedEndStation.getId(), ex.getMessage());
            }
            throw ex;
        }
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

        double discountRate = 0.0;
        try {
            LoyaltyTier tier = loyaltyRepository != null && editedTrip.getRider() != null ?
                    loyaltyRepository.findCurrentTier(editedTrip.getRider().getUserId()) : LoyaltyTier.ENTRY;
                    
            switch (tier) {
                case GOLD : discountRate = 0.15;
                break;
                case SILVER : discountRate = 0.10;
                break;
                case BRONZE : discountRate =0.05;
                break;
                default : discountRate = 0.0;
            }
        
        } catch (Exception e) {
            logger.warn("Failed to determine loyalty tier", e);
            discountRate = 0.0;
        }

        editedTrip.setAppliedDiscountRate(discountRate);

        // SELECT PRICING STRATEGY based on rider plan
        PricingPlan.PlanType planType = resolvePlanType(editedTrip.getRider());
        PricingStrategyRepository strategy = selectStrategy(planType);
        PricingPlan pricingPlan = DefaultPricingPlans.planForType(planType);
        String planName = planType.name();
        
        // Calculate bill using strategy
        Bill bill = strategy.calculate(editedTrip, pricingPlan, editedTrip.getAppliedDiscountRate());

        // Create and persist ledger entry
        LedgerEntry ledgerEntry = new LedgerEntry(editedTrip.getRider(), editedTrip, bill, planName);
        ledgerEntryRepository.save(ledgerEntry);

        discountRate = editedTrip.getAppliedDiscountRate();
        double preDiscountTotal = bill.getBaseCost() + bill.getTimeCost() + bill.getEBikeSurcharge();
        double discountAmount = Math.max(0.0, preDiscountTotal - bill.getTotalCost());

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
                planName,
                discountRate,
                discountAmount
        ));

        //Check and add flex credit
        double endStationRequiredFreeDock = 0.75; //75% of the docks is empty after finish docking
        double creditPercentage = 0.05; // Give you 5% credit back
        if (endStation.getFreeDockCount() > endStation.getCapacity()*endStationRequiredFreeDock) {
            logger.info("User dock in lightly occupied station, add credit");
            User user = userRepository.findById(editedTrip.getRider().getUserId());
            double amountToAdd = Math.floor(bill.getTotalCost()*creditPercentage*100) / 100; // Round to 2 decimal case
            user.addFlexCredit(amountToAdd);
            userRepository.save(user);
            eventPublisher.publish(new FlexCreditAddedEvent(editedTrip.getRider().getUserId(), amountToAdd));
            logger.info("User received "+amountToAdd+" credit");
        }
        return ledgerEntry;
    }

    private void validate(Trip currentTrip, Station station) {
        if (station.isOutOfService()) {
            logger.error("Destination station is out of service");
            throw new IllegalStateException("Destination station is out of service");
        }
        if (!station.hasFreeDock()){
            logger.warn("Destination station {} has no free docks", station.getId());
            throw new StationFullException(station.getId());
        }
    }

    private PricingStrategyRepository selectStrategy(PricingPlan.PlanType planType) {
        if (planType == PricingPlan.PlanType.MONTHLY_SUBSCRIBER) {
            return monthlySubscriberStrategy;
        }
        return payAsYouGoStrategy;
    }

    private PricingPlan.PlanType resolvePlanType(Rider rider) {
        PricingPlan.PlanType planType = rider != null ? rider.getPricingPlanType() : null;
        if (planType == null) {
            return PricingPlan.PlanType.PAY_AS_YOU_GO;
        }
        return planType;
    }

}
