package dk.dtu.scout.backend.dto.catalog;

public record ParamDef(
        String key,
        String label,
        String type,
        Object defaultValue,
        Double min,
        Double max
) {}