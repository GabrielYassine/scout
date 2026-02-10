package dk.dtu.scout.backend.dto;

import java.util.List;
import java.util.Map;

public record RunRequest(
   String searchSpaceId,
    Map<String, Object> searchSpaceParams,
    String problemId,
    Map<String, Object> problemParams,
    String algorithmId,
    Map<String, Object> algorithmParams,
    String mutationId,
    Map<String, Object> mutationParams,
    String populationModelId,
    Map<String, Object> populationModelParams,
    String acceptanceRuleId,
    Map<String, Object> acceptanceRuleParams,
    List<String> observerIds,
    String stopConditionId,
    Map<String, Object> stopConditionParams,
    long seed
) {}
