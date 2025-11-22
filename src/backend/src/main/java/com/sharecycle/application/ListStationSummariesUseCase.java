package com.sharecycle.application;

import com.sharecycle.domain.repository.JpaStationRepository;
import com.sharecycle.model.dto.StationSummaryDto;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListStationSummariesUseCase {
    private final JpaStationRepository stationRepository;

    public ListStationSummariesUseCase(JpaStationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    @Transactional
    public List<StationSummaryDto> execute() {
        return stationRepository.findAll().stream()
                .map(station -> new StationSummaryDto(
                        station.getId(),
                        station.getName(),
                        station.getStatus(),
                        station.getAvailableBikeCount(),
                        station.getBikesDocked(),
                        station.getEBikesDocked(),
                        station.getEBikesAvailable(),
                        station.getCapacity(),
                        station.getFreeDockCount(),
                        station.getLatitude(),
                        station.getLongitude(),
                        station.getFullnessCategory()
                ))
                .toList();
    }
}
