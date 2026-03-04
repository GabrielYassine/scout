package dk.dtu.scout.backend.dto.run;

import java.util.List;
import java.util.Map;

public record RunResponse(
    String problemId,
    List<Integer> iterations,
    List<Integer> evaluations,
    Map<String, List<?>> series,
    double runtimeMs,
    int finalEvaluations
) {}
