package com.sharecycle.ui;

import com.sharecycle.application.ListStationSummariesUseCase;
import com.sharecycle.model.dto.StationSummaryDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/stations")
public class PublicStationController {

    private final ListStationSummariesUseCase listStationSummariesUseCase;

    public PublicStationController(ListStationSummariesUseCase listStationSummariesUseCase) {
        this.listStationSummariesUseCase = listStationSummariesUseCase;
    }

    @GetMapping
    public List<StationSummaryDto> listStations() {
        return listStationSummariesUseCase.execute();
    }
}
