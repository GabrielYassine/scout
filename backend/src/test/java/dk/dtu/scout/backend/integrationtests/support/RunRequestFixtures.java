package dk.dtu.scout.backend.integrationtests.support;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RunRequestFixtures {

    private RunRequestFixtures() {
    }

    public static Map<String, Object> validRunPreparePayload(String sessionId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sessionId", sessionId);
        payload.put("executionType", "run");
        payload.put("runRequest", validBitstringRunRequest());
        payload.put("runtimeStudyRequest", null);
        return payload;
    }

    public static Map<String, Object> validRuntimeStudyPreparePayload(String sessionId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sessionId", sessionId);
        payload.put("executionType", "runtimeStudy");
        payload.put("runRequest", null);
        payload.put("runtimeStudyRequest", validRuntimeStudyRequest());
        return payload;
    }

    public static Map<String, Object> validBitstringRunRequest() {
        Map<String, Object> request = new LinkedHashMap<>();

        request.put("searchSpaceId", "bitstring");
        request.put("searchSpaceParams", mapOf("n", 10));

        request.put("problemIds", List.of("onemax"));
        request.put("problemParams", Map.of());

        request.put("generatorId", "bit-flip");
        request.put("generatorParams", mapOf("flipProbability", "1/n"));

        request.put("populationModelId", "mu-lambda");
        request.put("populationModelParams", mapOf("mu", 1, "lambda", 1));

        request.put("selectionRuleId", "mu-plus-lambda");
        request.put("selectionRuleParams", Map.of());

        request.put("parentSelectionRuleId", "elitist-parents");
        request.put("parentSelectionRuleParams", Map.of());

        request.put("crossoverId", null);
        request.put("crossoverParams", Map.of());

        request.put("stopConditionIds", List.of("max-evaluations"));
        request.put("stopConditionParams", mapOf(
                "max-evaluations",
                mapOf("maxEvaluations", 5)
        ));

        request.put("observerIds", List.of("fitness"));
        request.put("observerParams", Map.of());

        request.put("runTimes", 1);
        request.put("logEveryEvaluations", 1);
        request.put("wsUpdateEveryEvaluations", 1);

        return request;
    }

    public static Map<String, Object> validRuntimeStudyRequest() {
        Map<String, Object> request = new LinkedHashMap<>();

        request.put("searchSpaceId", "bitstring");
        request.put("searchSpaceParams", mapOf("n", 10));

        request.put("problemId", "onemax");
        request.put("problemParams", Map.of());

        request.put("generatorId", "bit-flip");
        request.put("generatorParams", mapOf("flipProbability", "1/n"));

        request.put("populationModelId", "mu-lambda");
        request.put("populationModelParams", mapOf("mu", 1, "lambda", 1));

        request.put("selectionRuleId", "mu-plus-lambda");
        request.put("selectionRuleParams", Map.of());

        request.put("parentSelectionRuleId", "elitist-parents");
        request.put("parentSelectionRuleParams", Map.of());

        request.put("crossoverId", null);
        request.put("crossoverParams", Map.of());

        request.put("stopConditionIds", List.of("optimum-reached"));
        request.put("stopConditionParams", Map.of());

        request.put("seed", 1234L);
        request.put("problemSizes", List.of(5, 10));
        request.put("repetitionsPerSize", 1);

        return request;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> runRequest(Map<String, Object> preparePayload) {
        return (Map<String, Object>) preparePayload.get("runRequest");
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> runtimeStudyRequest(Map<String, Object> preparePayload) {
        return (Map<String, Object>) preparePayload.get("runtimeStudyRequest");
    }

    public static Map<String, Object> withRunRequestField(
            Map<String, Object> preparePayload,
            String key,
            Object value
    ) {
        runRequest(preparePayload).put(key, value);
        return preparePayload;
    }

    public static Map<String, Object> withRuntimeStudyField(
            Map<String, Object> preparePayload,
            String key,
            Object value
    ) {
        runtimeStudyRequest(preparePayload).put(key, value);
        return preparePayload;
    }

    public static Map<String, Object> mapOf(Object... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("Expected an even number of key/value arguments.");
        }

        Map<String, Object> map = new LinkedHashMap<>();

        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }

        return map;
    }
}