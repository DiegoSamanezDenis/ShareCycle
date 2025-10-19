package com.sharecycle.application;

import com.sharecycle.domain.ReservationBuilder;
import com.sharecycle.domain.event.DomainEventPublisher;
import com.sharecycle.domain.event.ReservationCreatedEvent;
import com.sharecycle.domain.repository.JpaBikeRepository;
import com.sharecycle.domain.repository.ReservationRepository;
import com.sharecycle.domain.model.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReserveBikeUseCase {

    private JpaBikeRepository bikeRepository;
    private ReservationRepository reservationRepository;
    private final DomainEventPublisher eventPublisher;

    public ReserveBikeUseCase(JpaBikeRepository bikeRepository, ReservationRepository reservationRepository, DomainEventPublisher eventPublisher) {
        this.bikeRepository = bikeRepository;
        this.reservationRepository = reservationRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Reservation execute(Rider rider, Station station, Bike bike, int expiresAfterMinutes) {
        if (reservationRepository.existsByRiderId(rider.getUserId())) {
            throw new IllegalStateException("Rider already has an active reservation.");
        }
        if (station.getStatus().equals(Station.StationStatus.OUT_OF_SERVICE)) {
            throw new IllegalStateException("Station is not active.");
        }
        if (!(bike.getStatus().equals(Bike.BikeStatus.AVAILABLE))) {
            throw new IllegalStateException("Bike is not available.");
        }

        // Transition bike state using State Pattern
		bike.setStatus(Bike.BikeStatus.RESERVED);
		// Persist bike status change to ensure DB reflects reservation
		bikeRepository.save(bike);

        // Build and persist reservation
        Reservation reservation = new ReservationBuilder().rider(rider)
                .station(station)
                .bike(bike)
                .expiresAfterMinutes(expiresAfterMinutes)
                .build();

        reservationRepository.save(reservation);

        // Publish domain event
        eventPublisher.publish(new ReservationCreatedEvent(reservation.getReservationId(), rider.getUserId()));

        return reservation;
    }
}

