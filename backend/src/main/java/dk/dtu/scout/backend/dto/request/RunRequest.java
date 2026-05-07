package dk.dtu.scout.backend.dto.request;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for starting a normal run.
 * Contains the selected components, their parameters, and execution settings.
 * @author s235257 & s230632
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
    int logEveryEvaluations,
    int wsUpdateEveryEvaluations
 ) {}
