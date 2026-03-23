package dk.dtu.scout.backend.dto;

import java.util.List;
import java.util.Map;

public record RunRequest(
   // Search space and problem
   List<String> searchSpaceId,
    Map<String, Object> searchSpaceParams,
   List<String>  problemId,
    Map<String, Object> problemParams,

   // Variation-based algorithm parameters
   List<String>  generatorId,
    Map<String, Object> generatorParams,
   List<String>  populationModelId,
    Map<String, Object> populationModelParams,
   List<String>  acceptanceRuleId,
    Map<String, Object> acceptanceRuleParams,

   // Common parameters
    List<String> observerIds,
    Map<String, Object> observerParams,
    List<String> stopConditionId,
    Map<String, Object> stopConditionParams,
    long seed,
    int runTimes,
    int logEveryIterations
 ) {}
