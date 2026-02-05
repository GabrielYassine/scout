package dk.dtu.scout.backend.dto;

import java.util.Map;

public record RunRequest(String problemId, Map<String, Object> problemParams, String algorithmId, Map<String, Object> algorithmParams, long seed) {}
