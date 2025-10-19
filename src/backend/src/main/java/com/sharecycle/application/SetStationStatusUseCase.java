package com.sharecycle.application;

import com.sharecycle.domain.event.DomainEventPublisher;
import com.sharecycle.domain.event.StationStatusChangedEvent;
import com.sharecycle.domain.repository.JpaStationRepository;
import com.sharecycle.domain.repository.UserRepository;
import com.sharecycle.domain.model.Station;
import com.sharecycle.domain.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
public class SetStationStatusUseCase {

    private final JpaStationRepository stationRepository;
    private final UserRepository userRepository;
    private final DomainEventPublisher eventPublisher;

    public SetStationStatusUseCase(JpaStationRepository stationRepository,
                                   UserRepository userRepository,
                                   DomainEventPublisher eventPublisher) {
        this.stationRepository = stationRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Station execute(Command command) {
        Objects.requireNonNull(command, "command must not be null");

        requireOperator(command.operatorId());
        Station station = requireStation(command.stationId());

        if (command.outOfService()) {
            station.markOutOfService();
        } else {
            station.markActive();
        }

        stationRepository.save(station);

        eventPublisher.publish(new StationStatusChangedEvent(
                station.getId(),
                station.getStatus(),
                station.getCapacity(),
                station.getBikesDocked()
        ));

        return station;
    }

    private User requireOperator(UUID operatorId) {
        User user = userRepository.findById(operatorId);
        if (user == null || !"OPERATOR".equalsIgnoreCase(user.getRole())) {
            throw new SecurityException("Only operators can modify station status.");
        }
        return user;
    }

    private Station requireStation(UUID stationId) {
        Station station = stationRepository.findByIdForUpdate(stationId);
        if (station == null) {
            throw new IllegalArgumentException("Station not found.");
        }
        return station;
    }

    public record Command(UUID operatorId, UUID stationId, boolean outOfService) {}
}
