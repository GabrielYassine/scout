package dk.dtu.scout.backend.dto.run;

import java.util.List;

public record BatchRunResponse(
        List<RunGroupResponse> batches,
        BatchSummaryResponse summary
) {}