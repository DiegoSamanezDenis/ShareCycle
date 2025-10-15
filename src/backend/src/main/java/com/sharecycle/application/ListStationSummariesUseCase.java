package com.sharecycle.application;

import com.sharecycle.infrastructure.JpaStationRepositoryImpl;
import com.sharecycle.model.dto.StationSummaryDto;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListStationSummariesUseCase {
    private final JpaStationRepositoryImpl stationRepo;

    public ListStationSummariesUseCase(JpaStationRepositoryImpl stationRepo) {
        this.stationRepo = stationRepo;
    }

    @Transactional
    public List<StationSummaryDto> execute() {
        return stationRepo.findAll().stream()
                .map(station -> new StationSummaryDto(
                        station.getName(),
                        station.getBikesDocked(),
                        station.getCapacity()
                ))
                .toList();
    }

}
