package com.sharecycle.application;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sharecycle.domain.TripBuilder;
import com.sharecycle.domain.event.DomainEventPublisher;
import com.sharecycle.domain.event.TripStartedEvent;
import com.sharecycle.domain.model.Bike;
import com.sharecycle.domain.model.Reservation;
import com.sharecycle.domain.model.Rider;
import com.sharecycle.domain.model.Station;
import com.sharecycle.domain.model.Trip;
import com.sharecycle.domain.model.User;
import com.sharecycle.domain.repository.JpaBikeRepository;
import com.sharecycle.domain.repository.JpaStationRepository;
import com.sharecycle.domain.repository.ReservationRepository;
import com.sharecycle.domain.repository.TripRepository;
import com.sharecycle.domain.repository.UserRepository;
@Service
public class StartTripUseCase {
    private final JpaBikeRepository bikeRepository;
    private final JpaStationRepository stationRepository;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final ReservationRepository reservationRepository;
    private final DomainEventPublisher eventPublisher;
    public StartTripUseCase(JpaBikeRepository bikeRepository,
                            UserRepository userRepository,
                            JpaStationRepository stationRepository,
                            TripRepository tripRepository,
                            ReservationRepository reservationRepository,
                            DomainEventPublisher eventPublisher) {
        this.bikeRepository = bikeRepository;
        this.userRepository = userRepository;
        this.stationRepository = stationRepository;
        this.tripRepository = tripRepository;
        this.reservationRepository = reservationRepository;
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
        if (tripRepository.riderHasActiveTrip(managedRider.getUserId())) {
            throw new IllegalStateException("Rider already has an active trip.");
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
        Reservation activeReservation = reservationRepository.findByRiderId(managedRider.getUserId());
        if (activeReservation != null) {
            if (!activeReservation.getBike().getId().equals(managedBike.getId())) {
                throw new IllegalStateException("Rider must use the reserved bike.");
            }
        } else {
            if (managedBike.getStatus() == Bike.BikeStatus.RESERVED
                    || reservationRepository.hasActiveReservationForBike(managedBike.getId())) {
                throw new IllegalStateException("Bike is reserved by another rider.");
            }
            if (managedBike.getStatus() != Bike.BikeStatus.AVAILABLE) {
                throw new IllegalStateException("Bike is not available.");
            }
        }

        managedStartStation.undockBike(managedBike);
        managedBike.setStatus(Bike.BikeStatus.ON_TRIP);
        managedBike.setCurrentStation(null);
        managedBike.setReservationExpiry(null);
        // Persist station and bike state before inserting trip row to keep UI and DB in sync
        stationRepository.save(managedStartStation);
        bikeRepository.save(managedBike);

        TripBuilder tripBuilder = new TripBuilder();
        if (tripID != null) {
            tripBuilder.setTripId(tripID);
        }
        LocalDateTime effectiveStart = startTime != null ? startTime : LocalDateTime.now();
        tripBuilder.start(managedRider, managedStartStation, managedBike, effectiveStart);
        Trip trip = tripBuilder.build();

        // Now persist the trip (unique constraints on bike/user should be free because previous trip ended)
        tripRepository.save(trip);
        if (activeReservation != null) {
            activeReservation.expire();
            reservationRepository.save(activeReservation);
            managedBike.setReservationExpiry(null);
        }

        eventPublisher.publish(new TripStartedEvent(trip.getTripID(), trip.getStartTime(), trip.getEndTime(), trip.getDurationMinutes(),
                managedRider, managedBike, managedStartStation, null));

        return trip;
    }

}
