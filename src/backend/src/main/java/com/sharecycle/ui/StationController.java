package com.sharecycle.ui;

import com.sharecycle.application.BmsFacade;
import com.sharecycle.domain.model.User;
import com.sharecycle.domain.model.Station;
import com.sharecycle.model.dto.StationDetailsDto;
import com.sharecycle.model.dto.StationSummaryDto;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/stations")
public class StationController {

    private final BmsFacade bmsFacade;

    public StationController(BmsFacade bmsFacade) {
        this.bmsFacade = bmsFacade;
    }

    @GetMapping
    public List<StationSummaryDto> listStations() {
        return bmsFacade.listStations();
    }

    @GetMapping("/{stationId}/details")
    public StationDetailsDto getStationDetails(@PathVariable UUID stationId) {
        UUID principalId = extractPrincipalUserId();
        return bmsFacade.getStationDetails(stationId, principalId);
    }

    @PatchMapping("/{stationId}/status")
    public StationSummaryDto updateStatus(@PathVariable UUID stationId, @RequestBody UpdateStatusRequest request) {
        Station station = bmsFacade.updateStationStatus(request.operatorId(), stationId, request.outOfService());
        return toDto(station);
    }

    @PatchMapping("/{stationId}/capacity")
    public StationSummaryDto adjustCapacity(@PathVariable UUID stationId, @RequestBody AdjustCapacityRequest request) {
        Station station = bmsFacade.adjustStationCapacity(request.operatorId(), stationId, request.delta());
        return toDto(station);
    }

    @PostMapping("/move-bike")
    public List<StationSummaryDto> moveBike(@RequestBody MoveBikeRequest request) {
        bmsFacade.moveBike(request.operatorId(), request.bikeId(), request.destinationStationId());
        return bmsFacade.listStations();
    }

    private StationSummaryDto toDto(Station station) {
        return new StationSummaryDto(
                station.getId(),
                station.getName(),
                station.getStatus(),
                station.getAvailableBikeCount(),
                station.getBikesDocked(),
                station.getCapacity(),
                station.getFreeDockCount(),
                station.getLatitude(),
                station.getLongitude(),
                station.getFullnessCategory()
        );
    }

    private UUID extractPrincipalUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return user.getUserId();
        }
        return null;
    }
    public record UpdateStatusRequest(UUID operatorId, boolean outOfService) { }

    public record AdjustCapacityRequest(UUID operatorId, int delta) { }

    public record MoveBikeRequest(UUID operatorId, UUID bikeId, UUID destinationStationId) { }
}
