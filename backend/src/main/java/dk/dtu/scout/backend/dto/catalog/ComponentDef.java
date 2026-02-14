package dk.dtu.scout.backend.dto.catalog;

import java.util.List;

public record ComponentDef(
        String kind,
        String id,
        String displayName,
        String description,
        List<ParamDef> params,
        List<String> supportedSearchSpaces
) {}
