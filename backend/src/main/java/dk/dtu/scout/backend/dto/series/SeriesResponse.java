package dk.dtu.scout.backend.dto.series;

import dk.dtu.scout.logging.SeriesMode;

import java.util.List;

public record SeriesResponse<T>(
    SeriesMode mode,
    List<T> values
) {}