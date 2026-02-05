package dk.dtu.scout.backend.dto.catalog;

import java.util.List;

public record CatalogResponse(
    List<SearchSpaceDef> searchSpaces,
    List<ProblemDef> problems,
    List<AlgoDef> algorithms,
    List<MutationDef> mutations,
    List<AcceptanceRuleDef> acceptanceRules,
    List<StopConditionDef> stopConditions
) {}
