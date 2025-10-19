package com.sharecycle.application;

import com.sharecycle.domain.event.ReservationExpiredEvent;
import com.sharecycle.domain.repository.ReservationRepository;
import com.sharecycle.domain.repository.JpaBikeRepository;
import com.sharecycle.domain.event.DomainEventPublisher;
import com.sharecycle.domain.model.Bike;
import com.sharecycle.domain.model.Reservation;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Component
public class ReservationExpiryScheduler {

    private final ReservationRepository reservationRepository;
    private final JpaBikeRepository bikeRepository;
    private final DomainEventPublisher eventPublisher;

    public ReservationExpiryScheduler(ReservationRepository reservationRepository, JpaBikeRepository bikeRepository, DomainEventPublisher eventPublisher) {
        this.reservationRepository = reservationRepository;
        this.bikeRepository = bikeRepository;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(fixedRate = 60000) // every minute
    @Transactional
    public void expireReservations() {
        List<Reservation> expiredReservations = reservationRepository.findExpiredReservations();

        for (Reservation reservation : expiredReservations) {
            reservation.expire();
            reservation.getBike().setStatus(Bike.BikeStatus.AVAILABLE);
            // Persist the bike status flip to ensure DB reflects availability
            bikeRepository.save(reservation.getBike());
            reservationRepository.save(reservation);

            eventPublisher.publish(new ReservationExpiredEvent(reservation.getReservationId()));
        }
    }
}

