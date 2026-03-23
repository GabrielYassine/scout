package dk.dtu.scout.backend.dto.run;

import java.util.List;
import java.util.Map;

public record AverageRunResponse(
        List<Integer> iterations,
        List<Integer> evaluations,
        Map<String, List<Double>> series
) {}
