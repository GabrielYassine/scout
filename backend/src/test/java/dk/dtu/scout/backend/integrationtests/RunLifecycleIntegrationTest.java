package dk.dtu.scout.backend.integrationtests;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dtu.scout.backend.dto.request.PrepareRunResponse;
import dk.dtu.scout.backend.dto.ws.RunWsPayload;
import dk.dtu.scout.backend.dto.ws.RuntimeStudyWsPayload;
import dk.dtu.scout.backend.websocket.MergeOp;
import dk.dtu.scout.backend.websocket.WsReceiver;
import dk.dtu.scout.backend.websocket.WsSender;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static dk.dtu.scout.backend.integrationtests.support.BackendJsonTestSupport.mapValue;
import static dk.dtu.scout.backend.integrationtests.support.InstanceFixtures.tspInstancePayload;
import static dk.dtu.scout.backend.integrationtests.support.InstanceFixtures.vrpInstancePayload;
import static dk.dtu.scout.backend.integrationtests.support.RunPrepareTestSupport.prepareRun;
import static dk.dtu.scout.backend.integrationtests.support.RunPrepareTestSupport.prepareRuntimeStudy;
import static dk.dtu.scout.backend.integrationtests.support.RunRequestFixtures.validRunPreparePayload;
import static dk.dtu.scout.backend.integrationtests.support.RunRequestFixtures.validRuntimeStudyPreparePayload;
import static dk.dtu.scout.backend.integrationtests.support.WebSocketTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
class RunLifecycleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WsReceiver wsReceiver;

    @MockBean
    private WsSender wsSender;

    @Nested
    class StandardRunLifecycle {

        @Test
        void preparedBitstringRun_streamsProgressAndFinishesThroughWebSocket() throws Exception {
            PrepareRunResponse  prepared = prepareAndStartRun();

            List<RunWsPayload> payloads = captureRunPayloadsAfterFinished(wsSender, prepared.executionId());

            assertHasRunPayloadType(payloads, "RUN_PROGRESS");
            assertHasRunPayloadType(payloads, "RUN_FINISHED");

            RunWsPayload finishedProgress = finishedRunProgress(payloads);

            assertEquals(prepared.executionId(), finishedProgress.runId());
            assertEquals("bitstring", finishedProgress.searchSpaceId());
            assertEquals("onemax", finishedProgress.problemId());
            assertNotNull(finishedProgress.runtimeMs());
        }

        @Test
        void preparedBitstringRunWithMultipleProblems_executesRealBackendFlow() throws Exception {
            PrepareRunResponse  prepared = prepareAndStartRun(
                "lifecycle-session-multiple-problems",
                "ws-lifecycle-multiple-problems",
                runRequest -> runRequest.put("problemIds", List.of("onemax", "leadingones"))
            );

            List<RunWsPayload> payloads = captureRunPayloadsAfterFinished(wsSender, prepared.executionId());

            assertHasRunProgressForProblem(payloads, "onemax");
            assertHasRunProgressForProblem(payloads, "leadingones");
            assertHasRunPayloadType(payloads, "RUN_FINISHED");
        }

        @Test
        void preparedBitstringRunWithMultipleRunTimes_executesRealBackendFlow() throws Exception {
            PrepareRunResponse  prepared = prepareAndStartRun(
                "lifecycle-session-multiple-runtimes",
                "ws-lifecycle-multiple-runtimes",
                runRequest -> runRequest.put("runTimes", 2)
            );

            List<RunWsPayload> payloads = captureRunPayloadsAfterFinished(wsSender, prepared.executionId());

            assertHasRunProgressForRunIndex(payloads, 0);
            assertHasRunProgressForRunIndex(payloads, 1);
            assertHasRunPayloadType(payloads, "RUN_FINISHED");
        }

        @Test
        void preparedTspRun_startsThroughWebSocketAndExecutesRealBackendFlow() throws Exception {
            PrepareRunResponse  prepared = prepareAndStartRun(
                "lifecycle-session-tsp",
                "ws-lifecycle-tsp",
                RunLifecycleIntegrationTest::configureTspRun
            );

            List<RunWsPayload> payloads = captureRunPayloadsAfterFinished(wsSender, prepared.executionId());

            assertHasRunPayloadType(payloads, "RUN_FINISHED");

            RunWsPayload tspProgress = payloads.stream()
                .filter(payload -> "RUN_PROGRESS".equals(payload.type()))
                .filter(payload -> "tsp".equals(payload.problemId()))
                .filter(payload -> payload.seriesDelta() != null)
                .filter(payload -> payload.seriesDelta().containsKey("tspTour") || payload.seriesDelta().containsKey("tspCities"))
                .findFirst()
                .orElseThrow();

            assertTrue(tspProgress.seriesMerge().containsKey("tspTour") || tspProgress.seriesMerge().containsKey("tspCities"));
        }

        @Test
        void preparedRunWithEveryIterationWebSocketUpdates_doesNotDuplicateFinalEvaluation() throws Exception {
            PrepareRunResponse  prepared = prepareAndStartRun(
                "ws-final-no-duplicate-session",
                "ws-final-no-duplicate",
                runRequest -> {
                    runRequest.put("stopConditionIds", List.of("max-evaluations"));
                    runRequest.put("stopConditionParams", Map.of("maxEvaluations", 5));
                    runRequest.put("logEveryEvaluations", 1);
                    runRequest.put("wsUpdateEveryEvaluations", 1);
                }
            );

            List<RunWsPayload> payloads = captureRunPayloadsAfterFinished(wsSender, prepared.executionId());
            RunWsPayload finishedProgress = finishedRunProgress(payloads);

            long finalEvaluationCount = allProgressEvaluations(payloads).stream().filter(evaluation -> evaluation.equals(finishedProgress.evaluation())).count();
            assertEquals(1, finalEvaluationCount);
        }

        @Test
        void preparedRunWithSparseWebSocketUpdates_appendsFinalProgressPoint() throws Exception {
            PrepareRunResponse  prepared = prepareAndStartRun(
                "ws-final-append-session",
                "ws-final-append-session",
                runRequest -> {
                    runRequest.put("logEveryEvaluations", 1);
                    runRequest.put("wsUpdateEveryEvaluations", 1000);
                }
            );

            List<RunWsPayload> payloads = captureRunPayloadsAfterFinished(wsSender, prepared.executionId());

            assertTrue(payloads.stream()
                .filter(payload -> "RUN_PROGRESS".equals(payload.type()))
                .filter(payload -> "FINISHED".equals(payload.status()))
                .anyMatch(payload -> payload.evaluationsMerge() == MergeOp.APPEND));
        }

        @Test
        void preparedRunWithSeparatedLoggingAndWebSocketIntervals_sendsAccumulatedLoggedPoints() throws Exception {
            PrepareRunResponse  prepared = prepareAndStartRun(
                "ws-accumulated-points-session",
                "ws-accumulated-points",
                runRequest -> {
                    runRequest.put("stopConditionIds", List.of("max-evaluations"));
                    runRequest.put("stopConditionParams", Map.of("maxEvaluations", 12));
                    runRequest.put("logEveryEvaluations", 1);
                    runRequest.put("wsUpdateEveryEvaluations", 5);
                }
            );

            List<RunWsPayload> payloads = captureRunPayloadsAfterFinished(wsSender, prepared.executionId());

            assertTrue(payloads.stream()
                .filter(payload -> "RUN_PROGRESS".equals(payload.type()))
                .anyMatch(payload -> payload.evaluations() != null && payload.evaluations().size() > 1));
        }

        @Test
        void preparedRunWithFitnessPhaseObserver_streamsPhaseIntervals() throws Exception {
            PrepareRunResponse  prepared = prepareAndStartRun(
                "ws-fitness-phase-session",
                "ws-fitness-phase",
                RunLifecycleIntegrationTest::configureFitnessPhaseRun
            );

            List<RunWsPayload> payloads = captureRunPayloadsAfterFinished(wsSender, prepared.executionId());

            assertFalse(extractFitnessPhaseIntervals(payloads).isEmpty());
        }

        @Test
        void preparedRunWithFitnessPhaseObserver_doesNotDuplicatePhaseIntervals() throws Exception {
            PrepareRunResponse  prepared = prepareAndStartRun(
                "ws-fitness-phase-no-duplicates-session",
                "ws-fitness-phase-no-duplicates",
                RunLifecycleIntegrationTest::configureFitnessPhaseRun
            );

            List<Object> phaseIntervals = extractFitnessPhaseIntervals(captureRunPayloadsAfterFinished(wsSender, prepared.executionId()));

            long distinctCount = phaseIntervals.stream().map(Object::toString).distinct().count();

            assertEquals(distinctCount, phaseIntervals.size());
        }

        @Test
        void preparedRunWithUnknownGenerator_sendsFailedPayloadWhenStarted() throws Exception {
            PrepareRunResponse  prepared = prepareAndStartRun(
                "lifecycle-session-unknown-generator",
                "ws-lifecycle-unknown-generator",
                runRequest -> runRequest.put("generatorId", "does-not-exist")
            );

            List<RunWsPayload> payloads = captureRunPayloadsAfterFailed(wsSender, prepared.executionId());

            RunWsPayload failedPayload = payloads.stream()
                .filter(payload -> "RUN_FAILED".equals(payload.type()))
                .findFirst()
                .orElseThrow();

            assertTrue(failedPayload.message().contains("Unknown component: does-not-exist"));
        }

        @Test
        void preparedRunThatStopsAfterInitialLoggedPoint_replacesFinalProgressPoint() throws Exception {
            PrepareRunResponse  prepared = prepareAndStartRun(
                "ws-replace-last-initial-stop-session",
                "ws-replace-last-initial-stop",
                runRequest -> {
                    runRequest.put("searchSpaceParams", Map.of("n", 1));
                    runRequest.put("stopConditionIds", List.of("max-evaluations"));
                    runRequest.put("stopConditionParams", Map.of("maxEvaluations", 1));
                    runRequest.put("logEveryEvaluations", 1);
                    runRequest.put("wsUpdateEveryEvaluations", 1);
                }
            );

            List<RunWsPayload> payloads = captureRunPayloadsAfterFinished(wsSender, prepared.executionId());
            RunWsPayload finishedProgress = finishedRunProgress(payloads);

            assertEquals(MergeOp.REPLACE_LAST, finishedProgress.evaluationsMerge());
            assertEquals(List.of(finishedProgress.evaluation()), finishedProgress.evaluations());
        }

        @Test
        void preparedVrpRun_startsThroughWebSocketAndExecutesRealBackendFlow() throws Exception {
            PrepareRunResponse  prepared = prepareAndStartRun("lifecycle-session-vrp", "ws-lifecycle-vrp", RunLifecycleIntegrationTest::configureVrpRun);

            List<RunWsPayload> payloads = captureRunPayloadsAfterFinished(wsSender, prepared.executionId());
            String debugPayloads = debugPayloads(payloads);

            assertTrue(payloads.stream().anyMatch(payload -> "RUN_PROGRESS".equals(payload.type()) && "vrp".equals(payload.problemId())), debugPayloads);
            assertTrue(payloads.stream().anyMatch(payload -> "RUN_FINISHED".equals(payload.type())), debugPayloads);
        }
    }

    @Nested
    class RuntimeStudyLifecycle {

        @Test
        void preparedRuntimeStudy_startsThroughWebSocketAndExecutesRealBackendFlow() throws Exception {
            PrepareRunResponse  prepared = prepareAndStartStudy();

            List<RuntimeStudyWsPayload> payloads = captureStudyPayloadsAfterFinished(wsSender, prepared.executionId());

            assertTrue(payloads.stream().anyMatch(payload -> "STUDY_PROGRESS".equals(payload.type())));
            assertTrue(payloads.stream().anyMatch(payload -> "STUDY_FINISHED".equals(payload.type())));
        }
    }

    private PrepareRunResponse prepareAndStartRun() throws Exception {
        return prepareAndStartRun(
            "lifecycle-session-bitstring",
            "ws-lifecycle-bitstring",
            runRequest -> {}
        );
    }

    private PrepareRunResponse  prepareAndStartRun(String sessionId, String websocketSessionId, Consumer<Map<String, Object>> configure) throws Exception {
        Map<String, Object> payload = validRunPreparePayload(sessionId);
        configure.accept(mapValue(payload, "runRequest"));

        PrepareRunResponse  prepared = prepareRun(mockMvc, objectMapper, payload);

        startPreparedRun(wsReceiver, prepared, websocketSessionId);

        return prepared;
    }

    private PrepareRunResponse  prepareAndStartStudy() throws Exception {
        PrepareRunResponse  prepared = prepareRuntimeStudy(
            mockMvc,
            objectMapper,
            validRuntimeStudyPreparePayload("lifecycle-session-study")
        );

        startPreparedStudy(wsReceiver, prepared, "ws-lifecycle-study");

        return prepared;
    }

    private static List<Integer> allProgressEvaluations(List<RunWsPayload> payloads) {
        return payloads.stream()
            .filter(payload -> "RUN_PROGRESS".equals(payload.type()))
            .flatMap(payload -> {
                if (payload.evaluations() != null && !payload.evaluations().isEmpty()) {
                    return payload.evaluations().stream();
                }

                return Stream.of(payload.evaluation());
            })
            .toList();
    }

    private static List<Object> extractFitnessPhaseIntervals(List<RunWsPayload> payloads) {
        return payloads.stream()
            .filter(payload -> "RUN_PROGRESS".equals(payload.type()))
            .filter(payload -> payload.seriesDelta() != null)
            .filter(payload -> payload.seriesDelta().containsKey("fitnessPhaseIntervals"))
            .flatMap(payload -> {
                Object value = payload.seriesDelta().get("fitnessPhaseIntervals");
                if (value instanceof List<?> list) {
                    return list.stream();
                }
                return Stream.of(value);
            })
            .toList();
    }

    private static String debugPayloads(List<RunWsPayload> payloads) {
        return payloads.stream().map(payload -> "type=" + payload.type() + ", status=" + payload.status()
            + ", problemId=" + payload.problemId() + ", message=" + payload.message()).toList().toString();
    }

    private static void configureFitnessPhaseRun(Map<String, Object> runRequest) {
        runRequest.put("observerIds", List.of("fitness", "fitness-phase"));
        runRequest.put("observerParams", Map.of("windowSize", 3, "epsilon", 0.0));
        runRequest.put("stopConditionIds", List.of("max-evaluations"));
        runRequest.put("stopConditionParams", Map.of("maxEvaluations", 8));
        runRequest.put("logEveryEvaluations", 1);
        runRequest.put("wsUpdateEveryEvaluations", 1);
    }

    private static void configureTspRun(Map<String, Object> runRequest) {
        runRequest.put("searchSpaceId", "permutation");
        runRequest.put("searchSpaceParams", Map.of("n", 4));
        runRequest.put("problemIds", List.of("tsp"));
        runRequest.put("problemParams", Map.of("tspInstance", tspInstancePayload()));
        runRequest.put("generatorId", "2opt");
        runRequest.put("generatorParams", Map.of());
        runRequest.put("parentSelectionRuleId", "random-parents");
        runRequest.put("parentSelectionRuleParams", Map.of());
        runRequest.put("observerIds", List.of("fitness", "tour"));
        runRequest.put("stopConditionParams", Map.of("maxEvaluations", 3));
    }

    private static void configureVrpRun(Map<String, Object> runRequest) {
        runRequest.put("searchSpaceId", "route-list");
        runRequest.put("searchSpaceParams", Map.of("n", 2, "numberOfVehicles", 1));
        runRequest.put("problemIds", List.of("vrp"));
        runRequest.put("problemParams", Map.of("vrpInstance", vrpInstancePayload()));
        runRequest.put("generatorId", "route-list-relocate");
        runRequest.put("generatorParams", Map.of());
        runRequest.put("parentSelectionRuleId", "random-parents");
        runRequest.put("parentSelectionRuleParams", Map.of());
        runRequest.put("observerIds", List.of("fitness"));
        runRequest.put("stopConditionParams", Map.of("maxEvaluations", 3));
    }
}