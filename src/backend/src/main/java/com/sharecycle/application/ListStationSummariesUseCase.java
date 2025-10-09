package com.sharecycle.application;

import com.sharecycle.domain.repository.JpaStationRepository;
import com.sharecycle.model.dto.StationSummaryDto;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListStationSummariesUseCase {
    private final JpaStationRepository stationRepo;

    public ListStationSummariesUseCase(JpaStationRepository stationRepo) {
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
