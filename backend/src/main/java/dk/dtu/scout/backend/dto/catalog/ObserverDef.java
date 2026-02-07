package dk.dtu.scout.backend.dto.catalog;

import java.util.List;

public record ObserverDef(String id, String name, String description, List<ParamDef> params) implements CatalogItem {}
