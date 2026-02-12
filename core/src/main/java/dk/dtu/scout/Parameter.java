package dk.dtu.scout;

public record Parameter(
        String key,
        String label,
        String type,
        Object defaultValue,
        Double min,
        Double max
) {
}
