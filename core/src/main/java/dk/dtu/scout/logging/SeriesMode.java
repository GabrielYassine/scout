package dk.dtu.scout.logging;

/**
 * Defines how values are stored for a logged series.
 * ALL keeps every value added to the series.
 * LATEST_ONLY keeps only the most recent value, replacing the previous value.
 * @author s230632 & s235257
 */
public enum SeriesMode {
    ALL,
    LATEST_ONLY
}