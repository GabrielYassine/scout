package dk.dtu.scout.backend.dto;

import java.util.List;
import java.util.Map;

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




