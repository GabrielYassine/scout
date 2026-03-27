package dk.dtu.scout.backend.dto.run;

import dk.dtu.scout.backend.dto.series.SeriesResponse;

import java.util.List;
import java.util.Map;

public record RunResponse(
    String SearchSpaceId,
    String problemId,
    List<Integer> iterations,
    List<Integer> evaluations,
    Map<String, SeriesResponse<?>> series,
    double runtimeMs,
    int finalEvaluations
) {}
