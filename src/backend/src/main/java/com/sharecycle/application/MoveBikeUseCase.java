package com.sharecycle.application;

import com.sharecycle.domain.event.BikeMovedEvent;
import com.sharecycle.domain.event.BikeStatusChangedEvent;
import com.sharecycle.domain.event.DomainEventPublisher;
import com.sharecycle.domain.repository.JpaBikeRepository;
import com.sharecycle.domain.repository.JpaStationRepository;
import com.sharecycle.domain.repository.ReservationRepository;
import com.sharecycle.domain.repository.UserRepository;
import com.sharecycle.domain.model.Bike;
import com.sharecycle.domain.model.Station;
import com.sharecycle.domain.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
public class MoveBikeUseCase {

    private final JpaBikeRepository bikeRepository;
    private final JpaStationRepository stationRepository;
    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final DomainEventPublisher eventPublisher;

    public MoveBikeUseCase(JpaBikeRepository bikeRepository,
                           JpaStationRepository stationRepository,
                           UserRepository userRepository,
                           ReservationRepository reservationRepository,
                           DomainEventPublisher eventPublisher) {
        this.bikeRepository = bikeRepository;
        this.stationRepository = stationRepository;
        this.userRepository = userRepository;
        this.reservationRepository = reservationRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void execute(Command command) {
        Objects.requireNonNull(command, "command must not be null");

        requireOperator(command.operatorId());
        Bike bike = requireBike(command.bikeId());

        Station sourceStation = requireSourceStation(bike);
        Station destinationStation = requireDestinationStation(command.destinationStationId());

        ensureBikeBelongsToStation(bike, sourceStation);
        ensureStationIsOperational(sourceStation, "Source station is out of service.");
        ensureStationIsOperational(destinationStation, "Destination station is out of service.");
        ensureBikeIsMovable(bike);
        ensureBikeHasNoActiveReservation(bike);
        ensureDestinationHasCapacity(destinationStation);

        if (sourceStation.getId().equals(destinationStation.getId())) {
            throw new IllegalArgumentException("Source and destination stations must differ.");
        }

        sourceStation.undockBike(bike);
        destinationStation.dockBike(bike);
        bike.markAvailable();

        eventPublisher.publish(new BikeStatusChangedEvent(
                bike.getId(),
                bike.getStatus(),
                destinationStation.getId(),
                null
        ));
        stationRepository.save(sourceStation);
        stationRepository.save(destinationStation);
        bikeRepository.save(bike);

        eventPublisher.publish(new BikeMovedEvent(
                bike.getId(),
                sourceStation.getId(),
                destinationStation.getId()
        ));
    }

    private User requireOperator(UUID operatorId) {
        User user = userRepository.findById(operatorId);
        if (user == null || !"OPERATOR".equalsIgnoreCase(user.getRole())) {
            throw new SecurityException("Only operators can move bikes.");
        }
        return user;
    }

    private Bike requireBike(UUID bikeId) {
        Bike bike = bikeRepository.findById(bikeId);
        if (bike == null) {
            throw new IllegalArgumentException("Bike not found.");
        }
        return bike;
    }

    private Station requireSourceStation(Bike bike) {
        Station station = bike.getCurrentStation();
        if (station == null) {
            throw new IllegalStateException("Bike is not docked at any station.");
        }
        Station managed = stationRepository.findByIdForUpdate(station.getId());
        if (managed == null) {
            throw new IllegalStateException("Source station could not be loaded.");
        }
        return managed;
    }

    private Station requireDestinationStation(UUID stationId) {
        Station station = stationRepository.findByIdForUpdate(stationId);
        if (station == null) {
            throw new IllegalArgumentException("Destination station not found.");
        }
        return station;
    }

    private void ensureStationIsOperational(Station station, String message) {
        if (station.isOutOfService()) {
            throw new IllegalStateException(message);
        }
    }

    private void ensureBikeIsMovable(Bike bike) {
        if (!bike.isAvailable()) {
            throw new IllegalStateException("Bike is not available to move.");
        }
    }

    private void ensureBikeHasNoActiveReservation(Bike bike) {
        if (reservationRepository.hasActiveReservationForBike(bike.getId())) {
            throw new IllegalStateException("Bike has an active reservation and cannot be moved.");
        }
    }

    private void ensureDestinationHasCapacity(Station station) {
        if (station.findFirstEmptyDock().isEmpty()) {
            throw new IllegalStateException("Destination station has no free docks.");
        }
    }

    private void ensureBikeBelongsToStation(Bike bike, Station station) {
        if (station.findDockWithBike(bike.getId()).isEmpty()) {
            throw new IllegalStateException("Bike is not docked at the source station.");
        }
    }

    public record Command(UUID operatorId, UUID bikeId, UUID destinationStationId) {}
}
