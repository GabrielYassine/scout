package dk.dtu.scout.backend.dto;

import java.util.List;

public record BatchRunResponse (
    List<RunResponse> runs
) {}

