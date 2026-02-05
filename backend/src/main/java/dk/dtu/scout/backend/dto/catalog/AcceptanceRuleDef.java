package dk.dtu.scout.backend.dto.catalog;

import java.util.List;

public record AcceptanceRuleDef(String id, String name, String description, List<ParamDef> params) implements CatalogItem {}
