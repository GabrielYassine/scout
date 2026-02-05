package dk.dtu.scout.backend.dto.catalog;

import java.util.List;

public record CatalogResponse(List<ProblemDef> problems, List<AlgoDef> algorithms) {}
