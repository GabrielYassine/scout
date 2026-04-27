package dk.dtu.scout.backend.dto.request;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for starting a runtime study.
 * Contains the selected components, their parameters, and execution settings.
 * @author Ahmed
 */
public record RuntimeStudyRequest(
    String studyId,
    String sessionId,
    String searchSpaceId,
    Map<String, Object> searchSpaceParams,
    String problemId,
    Map<String, Object> problemParams,
    String generatorId,
    Map<String, Object> generatorParams,
    String selectionRuleId,
    Map<String, Object> selectionRuleParams,
    String populationModelId,
    Map<String, Object> populationModelParams,
    String parentSelectionRuleId,
    Map<String, Object> parentSelectionRuleParams,
    String crossoverId,
    Map<String, Object> crossoverParams,
    List<String> stopConditionIds,
    Map<String, Object> stopConditionParams,
    long seed,
    List<Integer> problemSizes,
    int repetitionsPerSize
) {}




