package dk.dtu.scout.backend.service;

import dk.dtu.scout.backend.dto.stats.SeriesPoint;
import dk.dtu.scout.backend.dto.stats.SeriesWindowStatsRequest;
import dk.dtu.scout.backend.dto.stats.SeriesWindowStatsResponse;
import dk.dtu.scout.backend.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StatisticsServiceTest {

    private final StatsService service = new StatsService();

    @Test
    void computeSeriesWindowStats_returnsSummaryForMatchingWindow() {
        SeriesWindowStatsResponse response = service.computeSeriesWindowStats(
                new SeriesWindowStatsRequest(
                        "fitness",
                        "Evaluation",
                        "Fitness",
                        2,
                        4,
                        List.of(
                                new SeriesPoint(1, 10),
                                new SeriesPoint(2, 8),
                                new SeriesPoint(3, 6),
                                new SeriesPoint(4, 4),
                                new SeriesPoint(5, 2)
                        )
                )
        );

        assertEquals(3, response.count());
        assertEquals(4.0, response.min(), 1e-9);
        assertEquals(8.0, response.max(), 1e-9);
        assertEquals(6.0, response.mean(), 1e-9);
        assertEquals(6.0, response.median(), 1e-9);
        assertEquals("down", response.trend());
    }

    @Test
    void computeSeriesWindowStats_throwsWhenWindowHasNoPoints() {
        assertThrows(BadRequestException.class, () -> service.computeSeriesWindowStats(
                new SeriesWindowStatsRequest(
                        "fitness",
                        "Evaluation",
                        "Fitness",
                        100,
                        200,
                        List.of(new SeriesPoint(1, 10), new SeriesPoint(2, 8))
                )
        ));
    }

    @Test
    void computeSeriesWindowStats_throwsWhenRangeIsInvalid() {
        assertThrows(BadRequestException.class, () -> service.computeSeriesWindowStats(
                new SeriesWindowStatsRequest(
                        "fitness",
                        "Evaluation",
                        "Fitness",
                        5,
                        1,
                        List.of(new SeriesPoint(1, 10), new SeriesPoint(2, 8))
                )
        ));
    }
}
