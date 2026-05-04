package dk.dtu.scout.backend.controller;

import dk.dtu.scout.backend.dto.request.RunRequest;
import dk.dtu.scout.backend.dto.run.RunGroupResponse;
import dk.dtu.scout.backend.dto.run.RunResponse;
import dk.dtu.scout.backend.dto.ws.RunWsPayload;
import dk.dtu.scout.backend.service.RunExecutor;
import dk.dtu.scout.backend.websocket.WsSender;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest
class RunExecutorIntegrationTest {

    @Autowired
    private RunExecutor runExecutor;

    @MockBean
    private WsSender wsSender;

    @Nested
    class StandardRunExecution {

        @Test
        void runBatch_executesTspRunWithRawInstancePayload() {
            RunRequest request = validTspRunRequest("run-executor-tsp");

            List<RunGroupResponse> result = runExecutor.runBatch(request, 1, 0);

            assertEquals(1, result.size());

            RunGroupResponse group = result.getFirst();
            assertEquals(1, group.runs().size());

            RunResponse run = group.runs().getFirst();
            assertEquals("permutation", run.searchSpaceId());
            assertEquals("tsp", run.problemId());
            assertFalse(run.evaluations().isEmpty());
            assertTrue(run.totalEvaluations() >= 0);
            assertTrue(run.runtimeMs() >= 0.0);
        }

        @Test
        void runBatch_executesVrpRunWithRawInstancePayload() {
            RunRequest request = validVrpRunRequest("run-executor-vrp");

            List<RunGroupResponse> result = runExecutor.runBatch(request, 1, 0);

            assertEquals(1, result.size());

            RunGroupResponse group = result.getFirst();
            assertEquals(1, group.runs().size());

            RunResponse run = group.runs().getFirst();
            assertEquals("route-list", run.searchSpaceId());
            assertEquals("vrp", run.problemId());
            assertFalse(run.evaluations().isEmpty());
            assertTrue(run.totalEvaluations() >= 0);
            assertTrue(run.runtimeMs() >= 0.0);
        }

        @Test
        void runBatch_executesSingleBitstringRun() {
            RunRequest request = validBitstringRunRequest("run-executor-single", 1, List.of("onemax"));

            List<RunGroupResponse> result = runExecutor.runBatch(request, 1, 0);

            assertEquals(1, result.size());

            RunGroupResponse group = result.getFirst();
            assertEquals(0, group.runIndex());
            assertEquals(1234L, group.seed());
            assertEquals(1, group.runs().size());

            RunResponse run = group.runs().getFirst();
            assertEquals("bitstring", run.searchSpaceId());
            assertEquals("onemax", run.problemId());
            assertFalse(run.evaluations().isEmpty());
            assertTrue(run.totalEvaluations() >= 0);
            assertTrue(run.runtimeMs() >= 0.0);
            assertTrue(run.series().containsKey("fitness"));
        }

        @Test
        void runBatch_executesMultipleRunTimesAndKeepsRunIndexOrder() {
            RunRequest request = validBitstringRunRequest("run-executor-multiple", 3, List.of("onemax"));

            List<RunGroupResponse> result = runExecutor.runBatch(request, 1, 0);

            assertEquals(3, result.size());

            assertEquals(0, result.get(0).runIndex());
            assertEquals(1, result.get(1).runIndex());
            assertEquals(2, result.get(2).runIndex());

            assertEquals(1234L, result.get(0).seed());
            assertEquals(1235L, result.get(1).seed());
            assertEquals(1236L, result.get(2).seed());
        }

        @Test
        void runBatch_executesMultipleProblemsInSameRunIndex() {
            RunRequest request = validBitstringRunRequest("run-executor-multiple-problems", 1, List.of("onemax", "leadingones"));

            List<RunGroupResponse> result = runExecutor.runBatch(request, 1, 0);

            assertEquals(1, result.size());

            RunGroupResponse group = result.getFirst();
            assertEquals(2, group.runs().size());

            assertEquals("onemax", group.runs().get(0).problemId());
            assertEquals("leadingones", group.runs().get(1).problemId());
        }

        @Test
        void runBatch_sendsProgressWhenWebSocketUpdatesAreEnabled() {
            RunRequest request = validBitstringRunRequest("run-executor-progress", 1, List.of("onemax"));

            List<RunGroupResponse> result = runExecutor.runBatch(request, 1, 1);

            assertEquals(1, result.size());

            verify(wsSender, timeout(2000).atLeastOnce()).sendToRun(eq("run-executor-progress"), any(RunWsPayload.class));
        }

        @Test
        void runBatch_acceptsNullProblemParamsForProblemWithoutInstance() {
            RunRequest request = new RunRequest(
                "bitstring",
                Map.of("n", 10),
                List.of("onemax"),
                null,
                "bit-flip",
                Map.of("flipProbability", "1/n"),
                "mu-lambda",
                Map.of("mu", 1, "lambda", 1),
                "mu-plus-lambda",
                Map.of(),
                "elitist-parents",
                Map.of(),
                null,
                null,
                List.of("fitness"),
                Map.of(),
                List.of("max-iterations"),
                Map.of("maxIterations", 5),
                1234L,
                1,
                "session-null-problem-params",
                "run-null-problem-params",
                1,
                0
            );

            List<RunGroupResponse> result = runExecutor.runBatch(request, 1, 0);

            assertEquals(1, result.size());
            assertEquals("onemax", result.getFirst().runs().getFirst().problemId());
        }
    }

    @Nested
    class InvalidRunExecution {

        @Test
        void runBatch_rejectsUnknownProblemComponent() {
            RunRequest request = new RunRequest(
                "bitstring",
                Map.of("n", 10),
                List.of("unknown-problem"),
                Map.of(),
                "bit-flip",
                Map.of("flipProbability", "1/n"),
                "mu-lambda",
                Map.of("mu", 1, "lambda", 1),
                "mu-plus-lambda",
                Map.of(),
                "elitist-parents",
                Map.of(),
                null,
                null,
                List.of("fitness"),
                Map.of(),
                List.of("max-iterations"),
                Map.of("maxIterations", 5),
                1234L,
                1,
                "session-invalid-problem",
                "run-invalid-problem",
                1,
                0
            );

            RuntimeException ex = assertThrows(RuntimeException.class, () ->
                runExecutor.runBatch(request, 1, 0)
            );

            assertTrue(ex.getMessage().contains("Unknown component"));
        }
    }

    private static RunRequest validBitstringRunRequest(String runId, int runTimes, List<String> problemIds) {
        return new RunRequest(
            "bitstring",
            Map.of("n", 10),
            problemIds,
            Map.of(),
            "bit-flip",
            Map.of("flipProbability", "1/n"),
            "mu-lambda",
            Map.of("mu", 1, "lambda", 1),
            "mu-plus-lambda",
            Map.of(),
            "elitist-parents",
            Map.of(),
            null,
            null,
            List.of("fitness"),
            Map.of(),
            List.of("max-iterations"),
            Map.of("maxIterations", 5),
            1234L,
            runTimes,
            "session-" + runId,
            runId,
            1,
            1
        );
    }


    private static RunRequest validTspRunRequest(String runId) {
        return new RunRequest(
            "permutation",
            Map.of("n", 4),
            List.of("tsp"),
            Map.of("tspInstance", validTspInstancePayload()),
            "2opt",
            Map.of(),
            "mu-lambda",
            Map.of("mu", 1, "lambda", 1),
            "mu-plus-lambda",
            Map.of(),
            "random-parents",
            Map.of(),
            null,
            null,
            List.of("fitness"),
            Map.of(),
            List.of("max-iterations"),
            Map.of("maxIterations", 3),
            1234L,
            1,
            "session-" + runId,
            runId,
            1,
            0
        );
    }

    private static RunRequest validVrpRunRequest(String runId) {
        return new RunRequest(
            "route-list",
            Map.of("n", 2),
            List.of("vrp"),
            Map.of("vrpInstance", validVrpInstancePayload()),
            "route-list-relocate",
            Map.of(),
            "mu-lambda",
            Map.of("mu", 1, "lambda", 1),
            "mu-plus-lambda",
            Map.of(),
            "random-parents",
            Map.of(),
            null,
            null,
            List.of("fitness"),
            Map.of(),
            List.of("max-iterations"),
            Map.of("maxIterations", 3),
            1234L,
            1,
            "session-" + runId,
            runId,
            1,
            0
        );
    }

    private static Map<String, Object> validTspInstancePayload() {
        Map<String, Object> tsp = new LinkedHashMap<>();
        tsp.put("name", "tiny");
        tsp.put("comment", "");
        tsp.put("cities", List.of(
            Map.of("x", 0.0, "y", 0.0),
            Map.of("x", 1.0, "y", 0.0),
            Map.of("x", 1.0, "y", 1.0),
            Map.of("x", 0.0, "y", 1.0)
        ));
        return tsp;
    }

    private static Map<String, Object> validVrpInstancePayload() {
        Map<String, Object> vrp = new LinkedHashMap<>();
        vrp.put("name", "tiny");
        vrp.put("comment", "");
        vrp.put("capacity", 10);
        vrp.put("numberOfVehicles", 1);
        vrp.put("depot", Map.of("x", 0.0, "y", 0.0));
        vrp.put("customers", List.of(
            Map.of("x", 1.0, "y", 1.0, "demand", 1.0),
            Map.of("x", 2.0, "y", 2.0, "demand", 1.0)
        ));
        return vrp;
    }
}