package dk.dtu.scout.dto;

import java.util.List;

/**
 * Metadata describing a configurable component parameter exposed to the UI.
 * @param key unique parameter key used in request parameter maps
 * @param label human-readable label shown in the UI
 * @param type parameter type, such as int, double, boolean, string, or enum
 * @param defaultValue default value shown or used for the parameter
 * @param min optional minimum value for numeric parameters
 * @param max optional maximum value for numeric parameters
 * @param options optional list of allowed values for enum-like parameters
 * @author s235257 & s230632
 */
public record Parameter(
        String key,
        String label,
        String type,
        Object defaultValue,
        Double min,
        Double max,
        List<String> options
) {}
