package dk.dtu.scout.backend.dto;

import java.util.List;

public record AlgoDef(String id, String name, List<ParamDef> params) {}