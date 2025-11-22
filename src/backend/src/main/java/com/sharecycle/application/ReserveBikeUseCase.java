package com.sharecycle.application;

import com.sharecycle.domain.ReservationBuilder;
import com.sharecycle.domain.event.DomainEventPublisher;
import com.sharecycle.domain.event.ReservationCreatedEvent;
import com.sharecycle.domain.repository.JpaBikeRepository;
import com.sharecycle.domain.repository.ReservationRepository;
import com.sharecycle.domain.repository.TripRepository;
import com.sharecycle.domain.model.*;
import com.sharecycle.domain.model.LoyaltyTier;
import com.sharecycle.domain.repository.JpaLoyaltyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReserveBikeUseCase {

    private final JpaBikeRepository bikeRepository;
    private final ReservationRepository reservationRepository;
    private final TripRepository tripRepository;
    private final DomainEventPublisher eventPublisher;
    private final JpaLoyaltyRepository loyaltyRepository;

    public ReserveBikeUseCase(JpaBikeRepository bikeRepository,
                              ReservationRepository reservationRepository,
                              TripRepository tripRepository,
                              DomainEventPublisher eventPublisher,
                              JpaLoyaltyRepository loyaltyRepository) {
        this.bikeRepository = bikeRepository;
        this.reservationRepository = reservationRepository;
        this.tripRepository = tripRepository;
        this.eventPublisher = eventPublisher;
        this.loyaltyRepository = loyaltyRepository;
    }

    @Transactional
    public Reservation execute(Rider rider, Station station, Bike bike, int expiresAfterMinutes) {
        if (reservationRepository.existsByRiderId(rider.getUserId())) {
            throw new IllegalStateException("Rider already has an active reservation.");
        }
        if (tripRepository.riderHasActiveTrip(rider.getUserId())) {
            throw new IllegalStateException("Rider already has an active trip.");
        }
        if (station.isOutOfService()) {
            throw new IllegalStateException("Station is not active.");
        }
        if (!bike.isAvailable()) {
            throw new IllegalStateException("Bike is not available.");
        }
        if (bike.getCurrentStation() == null || !station.getId().equals(bike.getCurrentStation().getId())) {
            throw new IllegalStateException("Bike is not docked at the requested station.");
        }
        if (station.findDockWithBike(bike.getId()).isEmpty()) {
            throw new IllegalStateException("Bike is not docked at the requested station.");
        }

        bike.reserve();

        int extraMinutes = 0;
        try {
            LoyaltyTier tier = loyaltyRepository != null ? loyaltyRepository.findCurrentTier(rider.getUserId()) : LoyaltyTier.ENTRY;
            if (tier == LoyaltyTier.SILVER) extraMinutes =2;
            if (tier == LoyaltyTier.GOLD) extraMinutes = 5;
        } catch (Exception e) {
            extraMinutes = 0;
        }
        int effectiveExpiry = expiresAfterMinutes + extraMinutes;

        // Build and persist reservation
        Reservation reservation = new ReservationBuilder().rider(rider)
                .station(station)
                .bike(bike)
                .expiresAfterMinutes(effectiveExpiry)
                .build();

        bike.setReservationExpiry(reservation.getExpiresAt());
        bikeRepository.save(bike);
        reservationRepository.save(reservation);

        // Publish domain event
        eventPublisher.publish(new ReservationCreatedEvent(reservation.getReservationId(), rider.getUserId()));

        return reservation;
    }
}

