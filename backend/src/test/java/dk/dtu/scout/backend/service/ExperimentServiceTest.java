package dk.dtu.scout.backend.service;

import dk.dtu.scout.backend.dto.RunRequest;
import dk.dtu.scout.backend.dto.run.BatchRunResponse;
import dk.dtu.scout.backend.util.TSPLibParser;
import dk.dtu.scout.backend.util.ViewMapper;
import dk.dtu.scout.problems.TSPInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ExperimentServiceTest {

    @Autowired
    private ExperimentService experimentService;

    private Map<String, Object> loadBerlin52Instance() throws IOException {
        String filePath = "src/test/resources/berlin52.tsp";
        String content = Files.readString(Paths.get(filePath));
        TSPInstance instance = TSPLibParser.parse(content);

        List<Map<String, Object>> cities = new ArrayList<>();
        double[][] coordinates = instance.getCoordinates();
        for (double[] coordinate : coordinates) {
            Map<String, Object> city = Map.of(
                    "x", coordinate[0],
                    "y", coordinate[1]
            );
            cities.add(city);
        }

        return Map.of(
                "name", instance.getName(),
                "cities", cities
        );
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
                List.of("bitstring"),
                Map.of("n", n),
                List.of(problemId),
                null,
                List.of("bit-flip"),
                Map.of("flipProbability", "1/n"),
                List.of("mu-lambda"),
                Map.of("lambda", 1),
                List.of("mu-plus-lambda"),
                null,
                List.of("fitness"),
                Map.of(),
                List.of("optimum-reached", "max-iterations"),
                Map.of("maxIterations", maxIterations),
                seed,
                runs,
                "test-run",
                100
        );

        BatchRunResponse response = experimentService.run(request);
        var stats = response.summary().runtimeByProblem().get(problemId);
        assertNotNull(stats);
        return stats.finalEvaluationsMedian();
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
        int runs = 20;
        int n1 = 128, n2 = 256;

        double m1 = runAndGetMedianFinalEvals("leadingones", n1, 67890L, runs, 20_000_000);
        double m2 = runAndGetMedianFinalEvals("leadingones", n2, 77890L, runs, 80_000_000);

        assertTrue(m1 > 0 && m2 > 0);

        double Robs = m2 / m1;
        double Rpred = Math.pow((double) n2 / n1, 2);

        // Here we check that the observed ratio is within a reasonable range of the predicted ratio
        // This lets us verify that with increasing n, the evaluation follow the expected n^2
        assertTrue(Robs >= 0.9 * Rpred && Robs <= 1.1 * Rpred, "LeadingOnes ratio mismatch: Robs=" + Robs + " Rpred=" + Rpred + " (m1=" + m1 + ", m2=" + m2 + ")");
        System.out.println("LeadingOnes: m1=" + m1 + ", m2=" + m2 + ", Robs=" + Robs + ", Rpred=" + Rpred);

        assertTrue(m1 < 0.5 * 20_000_000);
        assertTrue(m2 < 0.5 * 80_000_000);
    }

    @Test
    @DisplayName("TSP with 2-Opt generator and SA acceptance")
    void testTSPWith2OptAndSA() throws IOException {
        RunRequest request = ViewMapper.toRunRequest(
                 List.of("permutation"),
                 Map.of("n", 52),
                 List.of("tsp"),
                 Map.of("tspInstance", loadBerlin52Instance()),
                 List.of("2opt"),
                 Map.of(),
                 List.of("mu-lambda"),
                 Map.of("lambda", 20),
                 List.of("annealed-selection"),
                 Map.of(),
                 List.of("fitness", "tsp-tour"),
                 Map.of(),
                 List.of("max-iterations"),
                 Map.of("maxIterations", 20000),
                 12345L,
                 3,
                 "test-run-tsp-1",
                 100
         );

        BatchRunResponse response = experimentService.run(request);
    }

    @Test
    @DisplayName("TSP with Pheromone-Guided Mutation and Elitist acceptance")
    void testTSPWithPheromoneAndElitist() throws IOException {
        RunRequest request = ViewMapper.toRunRequest(
                 List.of("permutation"),
                 Map.of("n", 52),
                 List.of("tsp"),
                 Map.of("tspInstance", loadBerlin52Instance()),
                 List.of("pheromone-guided"),
                 Map.of("evaporationRate", 0.1, "alpha", 1.0, "beta", 2.0),
                 List.of("mu-lambda"),
                 Map.of("lambda", 20),
                 List.of("annealed-selection"),
                 Map.of(),
                 List.of("fitness", "tsp-tour"),
                 Map.of(),
                 List.of("max-iterations"),
                 Map.of("maxIterations", 10000),
                 67890L,
                 3,
                 "test-run-tsp-2",
                 100
         );

        BatchRunResponse response = experimentService.run(request);
    }
}
