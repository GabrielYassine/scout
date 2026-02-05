package dk.dtu.scout.backend.dto;

import java.util.Map;

public record RunRequest(String problemId, Map<String, Object> problemParams, String algorithmId, Map<String, Object> algorithmParams,  String mutationId, Map<String, Object> mutationParams,String acceptanceRuleId, Map<String, Object> acceptanceRuleParams, long seed) {}
