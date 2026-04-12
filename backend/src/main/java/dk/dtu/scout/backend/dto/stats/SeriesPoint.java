package dk.dtu.scout.backend.dto.stats;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
public record SeriesPoint(
        double x,
        double y
) {}
