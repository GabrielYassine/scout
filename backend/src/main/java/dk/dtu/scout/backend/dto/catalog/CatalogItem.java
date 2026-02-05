package dk.dtu.scout.backend.dto.catalog;

import java.util.List;

public interface CatalogItem {
    String id();
    String name();
    String description();
    List<ParamDef> params();
}
