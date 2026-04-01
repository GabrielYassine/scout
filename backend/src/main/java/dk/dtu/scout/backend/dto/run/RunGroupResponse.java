package dk.dtu.scout.backend.dto.run;

import java.util.List;

/**
 * Aggregates all problem runs for a single run index and seed.
 */
public record RunGroupResponse(
    int runIndex,
    long seed,
    List<RunResponse> runs
) {}
