package dk.dtu.scout.backend.dto.run;

import java.util.List;

public record BatchRunResponse(
        String runId,
        List<RunGroupResponse> batches,
        BatchSummaryResponse summary
) {}