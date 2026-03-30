package dk.dtu.scout.backend.dto;

import java.util.List;
import java.util.Map;

public record RunRequest(
    List<String> searchSpaceId,
    Map<String, Object> searchSpaceParams,
    List<String> problemId,
    Map<String, Object> problemParams,
    List<String> generatorId,
    Map<String, Object> generatorParams,
    List<String> populationModelId,
    Map<String, Object> populationModelParams,
    List<String> selectionRuleId,
    Map<String, Object> selectionRuleParams,
    List<String> parentSelectionRuleId,
    Map<String, Object> parentSelectionRuleParams,
    List<String> observerIds,
    Map<String, Object> observerParams,
    List<String> stopConditionId,
    Map<String, Object> stopConditionParams,
    long seed,
    int runTimes,
    String runId,
    int wsUpdateEveryIterations
) {}
