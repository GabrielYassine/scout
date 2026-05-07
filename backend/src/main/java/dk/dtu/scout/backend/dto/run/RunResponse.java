package dk.dtu.scout.backend.dto.run;

import java.util.List;
import java.util.Map;

/**
 * Result of a single run for a single problem.
 * @param searchSpaceId ID of the search space used
 * @param problemId ID of the problem solved
 * @param evaluations logged evaluation values
 * @param series logged observer series
 * @param runtimeMs runtime in milliseconds
 * @param totalEvaluations total number of evaluations performed when the run ended
 * @author s235257 & s230632
 */
public record RunResponse(
    String searchSpaceId,
    String problemId,
    List<Integer> evaluations,
    Map<String, SeriesResponse<?>> series,
    double runtimeMs,
    int totalEvaluations
) {}
