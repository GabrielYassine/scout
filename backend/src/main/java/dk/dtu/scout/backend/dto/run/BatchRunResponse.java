package dk.dtu.scout.backend.dto.run;

import java.util.List;

/**
 * Full response for a batch of runs, including summary statistics.
 */
public record BatchRunResponse(
        String runId,
        List<RunGroupResponse> batches,
        BatchSummaryResponse summary
) {}

