package dk.dtu.scout.backend.dto.catalog;

import java.util.List;

public record CatalogResponse(
    List<AlgorithmDef> algorithmTypes,
    List<ComponentDef> searchSpaces,
    List<ComponentDef> problems,
    List<ComponentDef> heuristicFunctions,
    List<ComponentDef> constructionPolicies,
    List<ComponentDef> pheromoneModels,
    List<ComponentDef> mutations,
    List<ComponentDef> acceptanceRules,
    List<ComponentDef> populationModels,
    List<ComponentDef> stopConditions,
    List<ComponentDef> observers
) {}
