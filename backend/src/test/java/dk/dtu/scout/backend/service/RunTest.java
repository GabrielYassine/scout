package dk.dtu.scout.backend.service;

import dk.dtu.scout.backend.dto.RunRequest;
import dk.dtu.scout.backend.dto.run.BatchRunResponse;
import dk.dtu.scout.backend.dto.run.RunResponse;
import dk.dtu.scout.backend.util.ViewMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RunTest {

    @Autowired
    private RunOrchestratorService runOrchestratorService;

    @Autowired
    private InstanceService instanceService;


    @SuppressWarnings("unchecked")
    private Map<String, Object> loadTspInstance(String resourceName) throws IOException {
        String content = Files.readString(Paths.get("src/test/resources/" + resourceName));
        Map<String, Object> response = instanceService.importInstance(content);
        return (Map<String, Object>) response.get("instance");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadVrpInstance(String resourceName) throws IOException {
        String content = Files.readString(Paths.get("src/test/resources/" + resourceName));
        Map<String, Object> response = instanceService.importInstance(content);
        return (Map<String, Object>) response.get("instance");
    }

    /**
     * Helper method to run the (1+1) EA on a given problem and return the median number of evaluations to reach the optimum.
     * @param problemId - the ID of the problem to run (e.g., "onemax" or "leadingones")
     * @param n - the problem size (number of bits)
     * @param seed - the random seed for reproducibility
     * @param runs - the number of independent runs to perform
     * @param maxIterations - the maximum number of iterations to allow (should be large enough to reach the optimum in most runs)
     * @return the median number of evaluations to reach the optimum across the runs
     */
    private double runAndGetMedianFinalEvals(String problemId, int n, long seed, int runs, int maxIterations) {
        RunRequest request = ViewMapper.toRunRequest(
            "bitstring",
            Map.of("n", n),

            List.of(problemId),
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

            List.of("max-iterations","optimum-reached"),
            Map.of("maxIterations", maxIterations),

            seed,
            runs,
            "test-session",
            "test-" + problemId,
            1,
            0
        );

        BatchRunResponse response = runOrchestratorService.run(request);
        List<Integer> finalEvals = response.batches().stream()
            .flatMap((batch) -> batch.runs().stream())
            .filter((run) -> problemId.equals(run.problemId()))
            .map(RunResponse::finalEvaluations)
            .sorted().toList();

        assertFalse(finalEvals.isEmpty());
        int mid = finalEvals.size() / 2;
        if (finalEvals.size() % 2 == 1) {
            return finalEvals.get(mid);
        }
        return (finalEvals.get(mid - 1) + finalEvals.get(mid)) / 2.0;
    }

    @Test
    @DisplayName("(1+1) EA on OneMax: scaling matches n ln n (ratio test)")
    void testOneMaxRatioScaling() {
        int runs = 20;
        int n1 = 256, n2 = 512;

        double m1 = runAndGetMedianFinalEvals("onemax", n1, 12345L, runs, 5_000_000);
        double m2 = runAndGetMedianFinalEvals("onemax", n2, 22345L, runs, 10_000_000);

        assertTrue(m1 > 0 && m2 > 0);

        double Robs = m2 / m1;
        double Rpred = (n2 * Math.log(n2)) / (n1 * Math.log(n1));

        // Here we check that the observed ratio is within a reasonable range of the predicted ratio
        // This lets us verify that with increasing n, the evaluation follow the expected n ln n
        assertTrue(Robs >= 0.9 * Rpred && Robs <= 1.1 * Rpred, "OneMax ratio mismatch: Robs=" + Robs + " Rpred=" + Rpred + " (m1=" + m1 + ", m2=" + m2 + ")");
        System.out.println("OneMax: m1=" + m1 + ", m2=" + m2 + ", Robs=" + Robs + ", Rpred=" + Rpred);

        assertTrue(m1 < 0.5 * 5_000_000);
        assertTrue(m2 < 0.5 * 10_000_000);
    }

    @Test
    @DisplayName("(1+1) EA on LeadingOnes: scaling matches n^2 (ratio test)")
    void testLeadingOnesRatioScaling() {
        int runs = 5;
        int n1 = 128, n2 = 256;

        // Keep iteration caps comfortably above expected runtime for these n values,
        // but bounded so the test can't run indefinitely on slower machines.
        double m1 = runAndGetMedianFinalEvals("leadingones", n1, 67890L, runs, 5_000_000);
        double m2 = runAndGetMedianFinalEvals("leadingones", n2, 77890L, runs, 20_000_000);

        assertTrue(m1 > 0 && m2 > 0);

        double Robs = m2 / m1;
        double Rpred = Math.pow((double) n2 / n1, 2);

        // Here we check that the observed ratio is within a reasonable range of the predicted ratio
        // This lets us verify that with increasing n, the evaluation follow the expected n^2
        assertTrue(Robs >= 0.9 * Rpred && Robs <= 1.1 * Rpred, "LeadingOnes ratio mismatch: Robs=" + Robs + " Rpred=" + Rpred + " (m1=" + m1 + ", m2=" + m2 + ")");
        System.out.println("LeadingOnes: m1=" + m1 + ", m2=" + m2 + ", Robs=" + Robs + ", Rpred=" + Rpred);

        assertTrue(m1 < 0.5 * 5_000_000);
        assertTrue(m2 < 0.5 * 20_000_000);
    }

    @Test
    @DisplayName("TSP with 2-Opt generator and SA acceptance")
    void testTSPWith2OptAndSA() throws IOException {
        RunRequest request = ViewMapper.toRunRequest(
            "permutation",
            Map.of("n", 52),
            List.of("tsp"),
            Map.of("tspInstance", loadTspInstance("berlin52.tsp")),
            "2opt",
            Map.of(),
            "mu-lambda",
            Map.of("lambda", 20),
            "annealed-selection",
            Map.of(),
            "random-parents",
            null,
            null,
            null,
            List.of("fitness", "tour"),
            Map.of(),
            List.of("max-iterations"),
            Map.of("maxIterations", 2000),
            12345L,
            1,
            "test-session",
            "test-run-tsp-1",
            100,
            0
        );

        BatchRunResponse response = runOrchestratorService.run(request);
        assertNotNull(response);
        assertNotNull(response.batches());
        assertFalse(response.batches().isEmpty());
    }

    @Test
    @DisplayName("TSP with TSP ACO generator and Elitist acceptance")
    void testTSPWithPheromoneAndElitist() throws IOException {
        RunRequest request = ViewMapper.toRunRequest(
            "permutation",
            Map.of("n", 52),
            List.of("tsp"),
            Map.of("tspInstance", loadTspInstance("berlin52.tsp")),
            "tsp-aco",
            Map.of("evaporationRate", 0.1, "alpha", 1.0, "beta", 2.0),
            "mu-lambda",
            Map.of("lambda", 20),
            "annealed-selection",
            Map.of(),
            "random-parents",
            null,
            null,
            null,
            List.of("fitness", "tour"),
            Map.of(),
            List.of("max-iterations"),
            Map.of("maxIterations", 10000),
            67890L,
            1,
            "test-session",
            "test-run-tsp-2",
            100,
            0
        );

        BatchRunResponse response = runOrchestratorService.run(request);
        assertNotNull(response);
        assertNotNull(response.batches());
        assertFalse(response.batches().isEmpty());
    }

    @Test
    @DisplayName("VRP X-n101-k25 with route-list swap (smoke test)")
    void testVrpXn101k25() throws IOException {
        Map<String, Object> vrpInstance = loadVrpInstance("x-n101-k25.vrp");

        RunRequest request = ViewMapper.toRunRequest(
            "route-list",
            Map.of("vrpInstance", vrpInstance),
            List.of("vrp"),
            Map.of("vrpInstance", vrpInstance),
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
            List.of("tour"),
            Map.of(),
            List.of("max-iterations"),
            Map.of("maxIterations", 1000),
            13579L,
            1,
            "test-session",
            "test-run-vrp-xn101-k25",
            100,
            0
        );

        BatchRunResponse response = runOrchestratorService.run(request);
        RunResponse vrpRun = response.batches().stream()
            .flatMap((batch) -> batch.runs().stream())
            .filter((run) -> "vrp".equals(run.problemId()))
            .findFirst()
            .orElseThrow();

        assertTrue(vrpRun.finalEvaluations() > 0);
        assertTrue(vrpRun.series().containsKey("tspCities"));
        assertTrue(vrpRun.series().containsKey("tspTour"));
    }
}
