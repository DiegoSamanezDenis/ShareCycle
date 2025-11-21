package com.sharecycle.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sharecycle.domain.ReservationBuilder;
import com.sharecycle.domain.event.DomainEventPublisher;
import com.sharecycle.domain.event.ReservationCreatedEvent;
import com.sharecycle.domain.model.Bike;
import com.sharecycle.domain.model.Reservation;
import com.sharecycle.domain.model.Rider;
import com.sharecycle.domain.model.Station;
import com.sharecycle.domain.model.User;
import com.sharecycle.domain.repository.JpaBikeRepository;
import com.sharecycle.domain.repository.ReservationRepository;
import com.sharecycle.domain.repository.TripRepository;

@Service
public class ReserveBikeUseCase {

    private final JpaBikeRepository bikeRepository;
    private final ReservationRepository reservationRepository;
    private final TripRepository tripRepository;
    private final DomainEventPublisher eventPublisher;

    public ReserveBikeUseCase(JpaBikeRepository bikeRepository,
                              ReservationRepository reservationRepository,
                              TripRepository tripRepository,
                              DomainEventPublisher eventPublisher) {
        this.bikeRepository = bikeRepository;
        this.reservationRepository = reservationRepository;
        this.tripRepository = tripRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Reservation execute(User user, Station station, Bike bike, int expiresAfterMinutes) {
        if (reservationRepository.existsByRiderId(user.getUserId())) {
            throw new IllegalStateException("Rider already has an active reservation.");
        }
        if (tripRepository.riderHasActiveTrip(user.getUserId())) {
            throw new IllegalStateException("Rider already has an active trip.");
        }
        if (station.isOutOfService()) {
            throw new IllegalStateException("Station is not active.");
        }
        if (!(bike.getStatus().equals(Bike.BikeStatus.AVAILABLE))) {
            throw new IllegalStateException("Bike is not available.");
        }
        if (bike.getCurrentStation() == null || !station.getId().equals(bike.getCurrentStation().getId())) {
            throw new IllegalStateException("Bike is not docked at the requested station.");
        }
        if (station.findDockWithBike(bike.getId()).isEmpty()) {
            throw new IllegalStateException("Bike is not docked at the requested station.");
        }

        // Transition bike state using State Pattern
		bike.setStatus(Bike.BikeStatus.RESERVED);

        // Create Rider representation for Reservation (domain model requires Rider)
        Rider riderForReservation = (user instanceof Rider r) ? r : new Rider(user);
        
        // Build and persist reservation
        Reservation reservation = new ReservationBuilder().rider(riderForReservation)
                .station(station)
                .bike(bike)
                .expiresAfterMinutes(expiresAfterMinutes)
                .build();

        bike.setReservationExpiry(reservation.getExpiresAt());
        bikeRepository.save(bike);
        reservationRepository.save(reservation);

        // Publish domain event
        eventPublisher.publish(new ReservationCreatedEvent(reservation.getReservationId(), user.getUserId()));

        return reservation;
    }
}

