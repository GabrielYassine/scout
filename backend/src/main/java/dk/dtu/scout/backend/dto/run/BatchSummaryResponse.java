package dk.dtu.scout.backend.dto.run;

import java.util.Map;

public record BatchSummaryResponse(
        Map<String, RuntimeStats> runtimeByProblem,
        Map<String, AverageRunResponse> averageByProblem
) {}

