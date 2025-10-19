package com.sharecycle.service;

import com.sharecycle.domain.ReservationBuilder;
import com.sharecycle.domain.repository.JpaBikeRepository;
import com.sharecycle.domain.repository.JpaStationRepository;
import com.sharecycle.domain.model.Reservation;
import com.sharecycle.domain.repository.ReservationRepository;
import com.sharecycle.domain.repository.UserRepository;
import com.sharecycle.domain.model.Rider;
import com.sharecycle.domain.model.Station;
import com.sharecycle.domain.model.Bike;
import com.sharecycle.domain.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class ReservationService {
    private ReservationRepository reservationRepository;
    private UserRepository userRepository;
    private JpaStationRepository jpaStationRepository;
    private JpaBikeRepository jpaBikeRepository;

    public ReservationService(ReservationRepository reservationRepository, UserRepository userRepository, JpaStationRepository jpaStationRepository, JpaBikeRepository jpaBikeRepository) {
        this.reservationRepository = reservationRepository;
        this.userRepository = userRepository;
        this.jpaStationRepository = jpaStationRepository;
        this.jpaBikeRepository = jpaBikeRepository;
    }

    @Transactional
    public Reservation reserveBike(UUID riderId, UUID stationId, UUID bikeId, int expiresAfterMinutes){
        User user = userRepository.findById(riderId);
        Station station = jpaStationRepository.findById(stationId);
        Bike bike = jpaBikeRepository.findById(bikeId);


        if (!(user instanceof Rider)) {
            throw new IllegalStateException("User with ID " + riderId + " is not a rider.");
        }

        Rider rider = (Rider) user;

        Reservation reservation = new ReservationBuilder().rider(rider).station(station).bike(bike).expiresAfterMinutes(expiresAfterMinutes).build();

        reservationRepository.save(reservation);
        return reservation;
    }
}
