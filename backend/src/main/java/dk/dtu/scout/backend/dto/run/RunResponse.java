package dk.dtu.scout.backend.dto.run;

import dk.dtu.scout.backend.dto.series.SeriesResponse;

import java.util.List;
import java.util.Map;

/**
 * Result of a single run for a single problem.
 */
public record RunResponse(
    String searchSpaceId,
    String problemId,
    List<Integer> iterations,
    List<Integer> evaluations,
    Map<String, SeriesResponse<?>> series,
    double runtimeMs,
    int totalEvaluations
) {}
