package dk.dtu.scout.backend.dto.stats;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * DTO for a point in a data series, consisting of an x and y value.
 * @author s235257
 */
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
public record SeriesPoint(
    double x,
    double y
) {}
