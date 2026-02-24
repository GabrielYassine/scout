package dk.dtu.scout.backend.dto;

import java.util.List;
import java.util.Map;

public record RunRequest(
   List<String> searchSpaceId,
    Map<String, Object> searchSpaceParams,
   List<String>  problemId,
    Map<String, Object> problemParams,
   List<String>  mutationId,
    Map<String, Object> mutationParams,
   List<String>  populationModelId,
    Map<String, Object> populationModelParams,
   List<String>  acceptanceRuleId,
    Map<String, Object> acceptanceRuleParams,
    List<String> observerIds,
    List<String> stopConditionId,
    Map<String, Object> stopConditionParams,
    long seed,
    int runTimes
) {}
