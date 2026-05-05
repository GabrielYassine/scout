package dk.dtu.scout.logging;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LoggedSeriesTest {

    @Test
    void add_keepsAllValuesWhenModeIsAll() {
        LoggedSeries<Integer> series = new LoggedSeries<>(SeriesMode.ALL);

        series.add(1);
        series.add(2);
        series.add(3);

        assertEquals(SeriesMode.ALL, series.getMode());
        assertEquals(List.of(1, 2, 3), series.getValues());
    }

    @Test
    void add_keepsOnlyLatestValueWhenModeIsLatestOnly() {
        LoggedSeries<Integer> series = new LoggedSeries<>(SeriesMode.LATEST_ONLY);

        series.add(1);
        series.add(2);
        series.add(3);

        assertEquals(SeriesMode.LATEST_ONLY, series.getMode());
        assertEquals(List.of(3), series.getValues());
    }

    @Test
    void getValues_returnsMutableBackingList() {
        LoggedSeries<Integer> series = new LoggedSeries<>(SeriesMode.ALL);

        series.getValues().add(10);

        assertEquals(List.of(10), series.getValues());
    }
}