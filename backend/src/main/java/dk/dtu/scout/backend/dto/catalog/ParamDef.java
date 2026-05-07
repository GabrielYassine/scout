package dk.dtu.scout.backend.dto.catalog;

import java.util.List;

/**
 * Defines a parameter for a Scout component.
 * @param key the unique key used in the backend to identify this parameter.
 * @param label the human-readable label shown in the frontend configuration UI.
 * @param type  the type of the parameter value.
 * @param defaultValue the default value for this parameter.
 * @param min optional minimum value for numeric parameters.
 * @param max optional maximum value for numeric parameters.
 * @author s235257 & s230632
 */
public record ParamDef(
    String key,
    String label,
    String type,
    Object defaultValue,
    Double min,
    Double max,
    List<String> options
) {}