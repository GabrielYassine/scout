package dk.dtu.scout.dto;

import java.util.List;

/**
 * Metadata describing a configurable component parameter exposed to the UI.
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
