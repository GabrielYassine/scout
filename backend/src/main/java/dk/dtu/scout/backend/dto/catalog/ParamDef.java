package dk.dtu.scout.backend.dto.catalog;

/**
 * Defines a parameter for a Scout component, including its key, label, type, default value, and optional min/max for numeric parameters.
 * @param key the unique key used in the backend to identify this parameter (e.g., "populationSize")
 * @param label the human-readable label shown in the frontend configuration UI (e.g., "Population Size")
 * @param type  the type of the parameter value (e.g., "integer", "double", "string", "boolean")
 * @param defaultValue the default value for this parameter, used if the user does not specify one
 * @param min optional minimum value for numeric parameters, used for validation in the frontend
 * @param max optional maximum value for numeric parameters, used for validation in the frontend
 * @author s235257 & Ahmed
 */
public record ParamDef(
    String key,
    String label,
    String type,
    Object defaultValue,
    Double min,
    Double max
) {}