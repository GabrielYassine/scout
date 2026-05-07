package dk.dtu.scout.backend.dto.run;

import java.util.List;

/**
 * DTO for a single runtime but multiple problems.
 * @author s235257 & s230632
 */
public record RunGroupResponse(
    int runIndex,
    long seed,
    List<RunResponse> runs
) {}
