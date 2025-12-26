package dk.dtu.scout.backend.dto;

import dk.dtu.scout.datatypes.IterationSnapshot;
import java.util.List;

public record RunResponse(String problemId, String algorithmId, List<IterationSnapshot> log) {}
