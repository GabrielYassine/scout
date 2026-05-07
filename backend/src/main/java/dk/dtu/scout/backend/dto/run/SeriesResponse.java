package dk.dtu.scout.backend.dto.run;

import dk.dtu.scout.logging.SeriesMode;

import java.util.List;

/**
 * DTO for a logged data series returned to the frontend.
 * @param mode how the frontend should treat the series, for example append-only or latest-only
 * @param values logged values for the series
 * @param <T> value type stored in the series
 * @author s235257 & s230632
 */
public record SeriesResponse<T>(
    SeriesMode mode,
    List<T> values
) {}