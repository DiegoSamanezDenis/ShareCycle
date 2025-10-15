package com.sharecycle.ui;

import com.sharecycle.application.ListStationSummariesUseCase;
import com.sharecycle.infrastructure.JpaStationRepositoryImpl;
import com.sharecycle.model.dto.StationSummaryDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class StationController {
    private final JpaStationRepositoryImpl jpaStationRepository;
    public StationController(JpaStationRepositoryImpl jpaStationRepository) {
        this.jpaStationRepository = jpaStationRepository;
    }
    @GetMapping("/api/stations")
    public List<StationSummaryDto> getStations() {
        ListStationSummariesUseCase listStationSummariesUseCase = new ListStationSummariesUseCase(jpaStationRepository);
        return listStationSummariesUseCase.execute();
    }
}
