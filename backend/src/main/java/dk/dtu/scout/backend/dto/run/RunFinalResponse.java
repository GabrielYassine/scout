package dk.dtu.scout.backend.dto.run;

public record RunFinalResponse(
        int runIndex,
        String problemId,
        double runtimeMs
) {}