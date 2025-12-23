package dk.dtu.scout.backend.dto;

import java.util.List;
public record ProblemDef(String id, String name, List<ParamDef> params) {}
