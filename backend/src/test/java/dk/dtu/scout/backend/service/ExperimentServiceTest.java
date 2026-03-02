package dk.dtu.scout.backend.service;

import dk.dtu.scout.backend.dto.RunRequest;
import dk.dtu.scout.backend.dto.run.BatchRunResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExperimentServiceTest {

    private TSPInstanceService tspInstanceService;
    private ExperimentService experimentService;

    @BeforeEach
    void setUp() {
        experimentService = new ExperimentService(new TSPInstanceService());
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
        RunRequest request = new RunRequest(
                List.of("bitstring"),
                Map.of("n", n),
                List.of(problemId),
                null,
                List.of("bit-flip"),
                Map.of("flipProbability", "1/n"),
                List.of("default"),
                Map.of("lambda", 1),
                List.of("elitist"),
                null,
                List.of("fitness"),
                List.of("optimum-reached", "max-iterations"),
                Map.of("maxIterations", maxIterations),
                seed,
                runs
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
}