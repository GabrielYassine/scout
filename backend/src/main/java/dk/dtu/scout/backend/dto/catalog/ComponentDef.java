package dk.dtu.scout.backend.dto.catalog;

import java.util.List;

/**
 * DTO representing a component definition in the catalog.
 * @param kind the type of component (e.g. "problem", "generator", etc.)
 * @param id the unique identifier of the component, used for instantiation
 * @param displayName the human-friendly name of the component to show in the UI
 * @param description a longer description of the component's purpose and behavior
 * @param params the list of parameters that can be configured for this component, including their types and default values
 * @param supportedSearchSpaces the list of search space types that this component is compatible with, used for frontend compatibility checks
 * @author s235257 & Ahmed
 */
public record ComponentDef(
    String kind,
    String id,
    String displayName,
    String description,
    List<ParamDef> params,
    List<String> supportedSearchSpaces
) {}
