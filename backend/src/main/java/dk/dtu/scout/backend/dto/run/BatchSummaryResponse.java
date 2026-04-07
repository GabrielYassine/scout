package dk.dtu.scout.backend.dto.run;

import java.util.Map;

/**
 * Summary statistics aggregated across a batch of runs.
 */
public record BatchSummaryResponse(
    Map<String, AverageRunResponse> averageByProblem,
    Map<String, SeriesBoxPlotResponse> bestFitnessBoxPlotsByProblem,
    Map<String,  Double> averageRunTimeByProblem
) {}
