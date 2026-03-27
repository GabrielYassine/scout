package dk.dtu.scout.backend.dto.catalog;

import java.util.List;

public record CatalogResponse(
    List<ComponentDef> searchSpaces,
    List<ComponentDef> problems,
    List<ComponentDef> generators,
    List<ComponentDef> selectionRules,
    List<ComponentDef> populationModels,
    List<ComponentDef> stopConditions,
    List<ComponentDef> observers
) {}
