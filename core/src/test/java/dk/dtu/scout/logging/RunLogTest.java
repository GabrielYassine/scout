package dk.dtu.scout.logging;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RunLogTest {

    @Test
    void tick_recordsEvaluationsInOrder() {
        RunLog log = new RunLog();

        log.tick(0);
        log.tick(5);
        log.tick(10);

        assertEquals(List.of(0, 5, 10), log.getEvaluations());
    }

    @Test
    void putSeries_createsSeriesAndAppendsValues() {
        RunLog log = new RunLog();

        log.putSeries("fitness", 1.0, SeriesMode.ALL);
        log.putSeries("fitness", 2.0, SeriesMode.ALL);

        assertEquals(List.of(1.0, 2.0), log.getSeries().get("fitness").getValues());
        assertEquals(SeriesMode.ALL, log.getSeries().get("fitness").getMode());
    }

    @Test
    void putSeries_reusesExistingSeriesMode() {
        RunLog log = new RunLog();

        log.putSeries("latest", "a", SeriesMode.LATEST_ONLY);
        log.putSeries("latest", "b", SeriesMode.ALL);

        assertEquals(List.of("b"), log.getSeries().get("latest").getValues());
        assertEquals(SeriesMode.LATEST_ONLY, log.getSeries().get("latest").getMode());
    }
}