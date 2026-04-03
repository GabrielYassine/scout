package dk.dtu.scout.backend.dto.run;

import java.util.List;

public record SeriesBoxPlotResponse(
        List<Integer> evaluations,
        List<List<Double>> boxplots
) {}