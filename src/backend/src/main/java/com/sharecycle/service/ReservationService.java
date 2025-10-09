package com.sharecycle.service;

import com.sharecycle.domain.ReservationBuilder;
import com.sharecycle.model.entity.Reservation;
import com.sharecycle.domain.repository.ReservationRepository;
import com.sharecycle.domain.repository.UserRepository;
import com.sharecycle.model.entity.Rider;
import com.sharecycle.model.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class ReservationService {
    private ReservationRepository reservationRepository;
    private UserRepository userRepository;

    public ReservationService(ReservationRepository reservationRepository, UserRepository userRepository) {
        this.reservationRepository = reservationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Reservation reserveBike(UUID riderId, int expiresAfterMinutes){
        User user = userRepository.findById(riderId);

        if (!(user instanceof Rider)) {
            throw new IllegalStateException("User with ID " + riderId + " is not a rider.");
        }

        Rider rider = (Rider) user;

        Reservation reservation = new ReservationBuilder().rider(rider).expiresAfterMinutes(expiresAfterMinutes).build();

        reservationRepository.save(reservation);
        return reservation;
    }
}
