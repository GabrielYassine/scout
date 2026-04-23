package dk.dtu.scout.backend.controller;

import dk.dtu.scout.backend.dto.stats.SeriesWindowStatsRequest;
import dk.dtu.scout.backend.dto.stats.SeriesWindowStatsResponse;
import dk.dtu.scout.backend.exception.BadRequestException;
import dk.dtu.scout.backend.service.SeriesStatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@CrossOrigin
public class StatsController {

    private final SeriesStatsService seriesStatsService;

    public StatsController(SeriesStatsService seriesStatsService) {
        this.seriesStatsService = seriesStatsService;
    }

    @PostMapping("/series-window")
    public ResponseEntity<SeriesWindowStatsResponse> seriesWindow(@RequestBody SeriesWindowStatsRequest request) {
        if (request == null) {
            throw new BadRequestException("Request body is required.");
        }
        return ResponseEntity.ok(seriesStatsService.computeSeriesWindowStats(request));
    }
}
