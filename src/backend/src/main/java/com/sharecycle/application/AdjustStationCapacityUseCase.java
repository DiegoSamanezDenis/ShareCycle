package com.sharecycle.application;

import com.sharecycle.domain.event.DomainEventPublisher;
import com.sharecycle.domain.event.StationCapacityChangedEvent;
import com.sharecycle.domain.repository.JpaStationRepository;
import com.sharecycle.domain.repository.UserRepository;
import com.sharecycle.model.entity.Station;
import com.sharecycle.model.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
public class AdjustStationCapacityUseCase {

    private final JpaStationRepository stationRepository;
    private final UserRepository userRepository;
    private final DomainEventPublisher eventPublisher;

    public AdjustStationCapacityUseCase(JpaStationRepository stationRepository,
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

        if (command.delta() == 0) {
            return station;
        }

        if (command.delta() > 0) {
            station.addEmptyDocks(command.delta());
        } else {
            station.removeEmptyDocks(Math.abs(command.delta()));
        }

        stationRepository.save(station);

        int freeDocks = station.getFreeDockCount();
        eventPublisher.publish(new StationCapacityChangedEvent(
                station.getId(),
                station.getCapacity(),
                station.getBikesDocked(),
                freeDocks
        ));

        return station;
    }

    private User requireOperator(UUID operatorId) {
        User user = userRepository.findById(operatorId);
        if (user == null || !"OPERATOR".equalsIgnoreCase(user.getRole())) {
            throw new SecurityException("Only operators can modify station capacity.");
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

    public record Command(UUID operatorId, UUID stationId, int delta) {}
}
