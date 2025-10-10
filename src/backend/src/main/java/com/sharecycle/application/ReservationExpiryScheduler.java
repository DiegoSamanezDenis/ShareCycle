package com.sharecycle.application;

import com.sharecycle.domain.event.ReservationExpiredEvent;
import com.sharecycle.domain.repository.ReservationRepository;
import com.sharecycle.domain.event.DomainEventPublisher;
import com.sharecycle.model.entity.Bike;
import com.sharecycle.model.entity.Reservation;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Component
public class ReservationExpiryScheduler {

    private final ReservationRepository reservationRepository;
    private final DomainEventPublisher eventPublisher;

    public ReservationExpiryScheduler(ReservationRepository reservationRepository, DomainEventPublisher eventPublisher) {
        this.reservationRepository = reservationRepository;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(fixedRate = 60000) // every minute
    @Transactional
    public void expireReservations() {
        List<Reservation> expiredReservations = reservationRepository.findExpiredReservations();

        for (Reservation reservation : expiredReservations) {
            reservation.expire();
            reservation.getBike().setStatus(Bike.BikeStatus.AVAILABLE);
            reservationRepository.save(reservation);

            eventPublisher.publish(new ReservationExpiredEvent(reservation.getReservationId()));
        }
    }
}

