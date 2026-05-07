package dk.dtu.scout.backend.dto.catalog;

import java.util.List;

/**
 * Response containing the available configurable SCOUT components.
 * @author s235257 & s230632
 */
public record CatalogResponse(
    List<ComponentDef> searchSpaces,
    List<ComponentDef> problems,
    List<ComponentDef> generators,
    List<ComponentDef> selectionRules,
    List<ComponentDef> populationModels,
    List<ComponentDef> parentSelectionRules,
    List<ComponentDef> crossovers,
    List<ComponentDef> stopConditions,
    List<ComponentDef> observers
) {}
