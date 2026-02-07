package dk.dtu.scout.backend.dto;

import java.util.List;
import java.util.Map;

public record RunResponse(
    String problemId,
    String algorithmId,
    List<Integer> iterations,
    Map<String, List<Double>> series
) {}
