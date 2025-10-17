package com.sharecycle.application;

import com.sharecycle.domain.event.TripStartedEvent;
import com.sharecycle.domain.TripBuilder;
import com.sharecycle.domain.event.DomainEventPublisher;
import com.sharecycle.domain.repository.JpaBikeRepository;
import com.sharecycle.domain.repository.JpaStationRepository;
import com.sharecycle.domain.repository.TripRepository;
import com.sharecycle.domain.repository.UserRepository;
import com.sharecycle.infrastructure.JpaUserRepository;
import com.sharecycle.model.entity.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.UUID;
@Service
public class StartTripUseCase {
    private JpaBikeRepository bikeRepository;
    private JpaStationRepository stationRepository;
    private JpaUserRepository userRepository;
    private TripRepository tripRepository;
    private final DomainEventPublisher eventPublisher;
    public StartTripUseCase(JpaBikeRepository bikeRepository, JpaUserRepository userRepository,JpaStationRepository stationRepository, TripRepository tripRepository, DomainEventPublisher eventPublisher) {
        this.bikeRepository = bikeRepository;
        this.userRepository = userRepository;
        this.stationRepository = stationRepository;
        this.tripRepository = tripRepository;
        this.eventPublisher = eventPublisher;
    }
    @Transactional
    public Trip execute(UUID tripID,
                        LocalDateTime startTime,
                        LocalDateTime endTime,
                        int durationMinutes,
                        Rider rider,
                        Bike bike,
                        Station startStation,
                        Station endStation) {

        if(!userRepository.existsByEmail(rider.getEmail())){
            throw new Error("User does not exist");
        }
        else if (startStation.getStatus().equals(Station.StationStatus.EMPTY)||startStation.getStatus().equals(Station.StationStatus.OUT_OF_SERVICE)) {
            throw new IllegalStateException("Station is empty.");
        }
        else if (!(bike.getStatus().equals(Bike.BikeStatus.AVAILABLE)||bike.getStatus().equals(Bike.BikeStatus.RESERVED))) {
            throw new IllegalStateException("Bike is not available.");
        }
        else {

            // Transition bike state
            bike.setStatus(Bike.BikeStatus.ON_TRIP);

            // Build and persist reservation
            TripBuilder tripBuilder = new TripBuilder();
            tripBuilder.start(rider, startStation, bike, startTime);
            tripBuilder.endAt(endStation, endTime);
            Trip trip = tripBuilder.build();
            tripRepository.save(trip);

            // Publish domain event
            startStation.setBikesDocked(startStation.getBikesDocked() - 1);
            eventPublisher.publish(new TripStartedEvent(tripID, startTime, endTime, durationMinutes, rider, bike, startStation, endStation));
            return trip;
        }
    }

}
