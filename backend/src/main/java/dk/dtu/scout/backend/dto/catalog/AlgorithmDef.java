package dk.dtu.scout.backend.dto.catalog;

import java.util.List;

public record AlgorithmDef(
    String id,
    String displayName,
    String description,
    List<String> componentTypes
) {}
