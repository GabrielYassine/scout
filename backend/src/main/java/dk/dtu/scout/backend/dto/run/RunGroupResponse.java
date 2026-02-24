package dk.dtu.scout.backend.dto.run;

import java.util.List;

public record RunGroupResponse(
    int runIndex,
    long seed,
    List<RunResponse> runs
) {}
