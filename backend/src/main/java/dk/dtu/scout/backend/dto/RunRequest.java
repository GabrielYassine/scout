package dk.dtu.scout.backend.dto;

import java.util.List;
import java.util.Map;

/**
 * Full run configuration submitted by the client.
 */
public record RunRequest(
   String searchSpaceId,
    Map<String, Object> searchSpaceParams,
   List<String> problemIds,
    Map<String, Object> problemParams,
   String  generatorId,
    Map<String, Object> generatorParams,
   String  populationModelId,
    Map<String, Object> populationModelParams,
    String  selectionRuleId,
    Map<String, Object> selectionRuleParams,
    String  parentSelectionRuleId,
    Map<String, Object> parentSelectionRuleParams,
    String  crossoverId,
    Map<String, Object> crossoverParams,
    List<String> observerIds,
    Map<String, Object> observerParams,
    List<String> stopConditionIds,
    Map<String, Object> stopConditionParams,
    long seed,
    int runTimes,
    String sessionId,
    String runId,
   int logEveryIterations,
    int wsUpdateEveryIterations
 ) {}
