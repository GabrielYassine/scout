package dk.dtu.scout.backend.dto;

import java.util.List;
import java.util.Map;

public record RunRequest(
   // Algorithm type (optional, defaults to "variation")
   String algorithmType,

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

   // Constructive algorithm parameters (ACO)
   List<String> pheromoneModelId,
    Map<String, Object> pheromoneModelParams,
   List<String> heuristicFunctionId,
    Map<String, Object> heuristicFunctionParams,
   List<String> constructionPolicyId,
    Map<String, Object> constructionPolicyParams,

   // Common parameters
    List<String> observerIds,
    List<String> stopConditionId,
    Map<String, Object> stopConditionParams,
    long seed,
    int runTimes
) {}
