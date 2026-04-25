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


/**
 * REST endpoint for computing statistics on runs.
 * @author s235257
 */
@RestController
@RequestMapping("/api/stats")
@CrossOrigin
public class StatsController {

    private final SeriesStatsService seriesStatsService;

    public StatsController(SeriesStatsService seriesStatsService) {
        this.seriesStatsService = seriesStatsService;
    }

    /**
     * Ending for computing statistics on an interval of a run.
     * @param request Contains the interval to compute the statistics on.
     * @return A DTO containing the computed statistics for the specified interval.
     */
    @PostMapping("/series-window")
    public ResponseEntity<SeriesWindowStatsResponse> seriesWindow(@RequestBody SeriesWindowStatsRequest request) {
        if (request == null) {
            throw new BadRequestException("Request body is required.");
        }
        return ResponseEntity.ok(seriesStatsService.computeSeriesWindowStats(request));
    }
}
