package com.sharecycle.application;

import com.sharecycle.domain.event.TripStartedEvent;
import com.sharecycle.domain.TripBuilder;
import com.sharecycle.domain.event.DomainEventPublisher;
import com.sharecycle.domain.repository.JpaBikeRepository;
import com.sharecycle.domain.repository.JpaStationRepository;
import com.sharecycle.domain.repository.TripRepository;
import com.sharecycle.domain.repository.UserRepository;
import com.sharecycle.domain.model.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.UUID;
@Service
public class StartTripUseCase {
    private JpaBikeRepository bikeRepository;
    private JpaStationRepository stationRepository;
    private UserRepository userRepository;
    private TripRepository tripRepository;
    private final DomainEventPublisher eventPublisher;
    public StartTripUseCase(JpaBikeRepository bikeRepository, UserRepository userRepository,JpaStationRepository stationRepository, TripRepository tripRepository, DomainEventPublisher eventPublisher) {
        this.bikeRepository = bikeRepository;
        this.userRepository = userRepository;
        this.stationRepository = stationRepository;
        this.tripRepository = tripRepository;
        this.eventPublisher = eventPublisher;
    }
    @Transactional
    public Trip execute(UUID tripID,
                        LocalDateTime startTime,
                        int durationMinutes,
                        Rider rider,
                        Bike bike,
                        Station startStation) {

        User user = userRepository.findById(rider.getUserId());
        if (!(user instanceof Rider managedRider)) {
            throw new IllegalStateException("Only riders can start trips.");
        }

        Bike managedBike = bikeRepository.findById(bike.getId());
        if (managedBike == null) {
            throw new IllegalArgumentException("Bike not found.");
        }

        Station managedStartStation = stationRepository.findByIdForUpdate(startStation.getId());
        if (managedStartStation == null) {
            throw new IllegalArgumentException("Start station not found.");
        }

        if (managedStartStation.isOutOfService()) {
            throw new IllegalStateException("Station is out of service.");
        }
        if (!managedStartStation.hasAvailableBike()) {
            throw new IllegalStateException("Station has no available bikes.");
        }
        if (managedStartStation.findDockWithBike(managedBike.getId()).isEmpty()) {
            throw new IllegalStateException("Bike is not docked at the specified station.");
        }
        if (!(managedBike.getStatus() == Bike.BikeStatus.AVAILABLE
                || managedBike.getStatus() == Bike.BikeStatus.RESERVED)) {
            throw new IllegalStateException("Bike is not available.");
        }

        managedStartStation.undockBike(managedBike);
        managedBike.setStatus(Bike.BikeStatus.ON_TRIP);
        managedBike.setCurrentStation(null);

        TripBuilder tripBuilder = new TripBuilder();
        if (tripID != null) {
            tripBuilder.setTripId(tripID);
        }
        tripBuilder.start(managedRider, managedStartStation, managedBike, startTime);
        Trip trip = tripBuilder.build();

        tripRepository.save(trip);
        stationRepository.save(managedStartStation);
        bikeRepository.save(managedBike);

        eventPublisher.publish(new TripStartedEvent(trip.getTripID(), trip.getStartTime(), trip.getEndTime(), trip.getDurationMinutes(),
                managedRider, managedBike, managedStartStation, null));

        return trip;
    }

}
