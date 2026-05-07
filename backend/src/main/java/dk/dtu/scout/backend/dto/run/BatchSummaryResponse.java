package dk.dtu.scout.backend.dto.run;

import java.util.Map;

/**
 * DTO for summarizing the results of a batch of runs across multiple problems.
 * @author s230632
 */
public record BatchSummaryResponse(
    Map<String, AverageRunResponse> averageByProblem,
    Map<String, SeriesBoxPlotResponse> bestFitnessBoxPlotsByProblem,
    Map<String,  Double> averageRunTimeByProblem
) {}
