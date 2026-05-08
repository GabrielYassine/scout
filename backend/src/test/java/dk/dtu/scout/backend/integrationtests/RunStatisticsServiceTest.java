package dk.dtu.scout.backend.integrationtests;

import dk.dtu.scout.backend.dto.run.AverageRunResponse;
import dk.dtu.scout.backend.dto.run.BatchSummaryResponse;
import dk.dtu.scout.backend.dto.run.RunGroupResponse;
import dk.dtu.scout.backend.dto.run.RunResponse;
import dk.dtu.scout.backend.dto.run.RuntimeStudyPointResponse;
import dk.dtu.scout.backend.dto.run.SeriesBoxPlotResponse;
import dk.dtu.scout.backend.dto.run.SeriesResponse;
import dk.dtu.scout.backend.service.RunStatisticsService;
import dk.dtu.scout.logging.SeriesMode;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class RunStatisticsServiceTest {

    private final RunStatisticsService service = new RunStatisticsService();

    @Nested
    class CalculateSummary {

        @Test
        void calculateSummary_groupsRunsByProblemAndComputesAverageRuntime() {
            RunResponse onemaxA = run("onemax", List.of(0, 1, 2), Map.of("fitness", series(1, 2, 3)), 10.0, 3);
            RunResponse onemaxB = run("onemax", List.of(0, 1, 2), Map.of("fitness", series(3, 4, 5)), 30.0, 3);
            RunResponse leadingOnes = run("leadingones", List.of(0, 1), Map.of("fitness", series(7, 8)), 20.0, 2);
            BatchSummaryResponse summary = service.calculateSummary(List.of(group(0, onemaxA, leadingOnes), group(1, onemaxB)));

            assertEquals(2, summary.averageByProblem().size());

            AverageRunResponse onemaxAverage = summary.averageByProblem().get("onemax");
            AverageRunResponse leadingOnesAverage = summary.averageByProblem().get("leadingones");

            assertNotNull(onemaxAverage);
            assertNotNull(leadingOnesAverage);
            assertEquals(20.0, summary.averageRunTimeByProblem().get("onemax"));
            assertEquals(20.0, summary.averageRunTimeByProblem().get("leadingones"));
            assertEquals(List.of(0, 1, 2), onemaxAverage.evaluations());
            assertEquals(List.of(2.0, 3.0, 4.0), onemaxAverage.series().get("fitness"));
            assertEquals(List.of(0, 1), leadingOnesAverage.evaluations());
            assertEquals(List.of(7.0, 8.0), leadingOnesAverage.series().get("fitness"));
        }

        @Test
        void calculateSummary_usesLongestEvaluationAxisAsReferenceRun() {
            RunResponse shortRun = run("onemax", List.of(0, 2), Map.of("fitness", series(10, 20)), 10.0, 2);
            RunResponse longRun = run("onemax", List.of(0, 1, 2, 3), Map.of("fitness", series(2, 4, 6, 8)), 10.0, 4);

            BatchSummaryResponse summary = service.calculateSummary(List.of(group(0, shortRun), group(1, longRun)));
            AverageRunResponse average = summary.averageByProblem().get("onemax");

            assertEquals(List.of(0, 1, 2, 3), average.evaluations());
            assertEquals(List.of(6.0, 7.0, 13.0, 14.0), average.series().get("fitness"));
        }

        @Test
        void calculateSummary_averagesOnlyWhitelistedNumericSeries() {
            Map<String, SeriesResponse<?>> series = new LinkedHashMap<>();
            series.put("fitness", series(1, 2, 3));
            series.put("bestFitness", series(1, 2, 3));
            series.put("temperature", series(100, 50, 25));
            series.put("label", new SeriesResponse<>(SeriesMode.ALL, List.of("a", "b", "c")));

            RunResponse run = run("onemax", List.of(0, 1, 2), series, 10.0, 3);

            BatchSummaryResponse summary = service.calculateSummary(List.of(group(0, run)));

            Map<String, List<Double>> averageSeries = summary.averageByProblem().get("onemax").series();

            assertTrue(averageSeries.containsKey("fitness"));
            assertTrue(averageSeries.containsKey("bestFitness"));
            assertFalse(averageSeries.containsKey("temperature"));
            assertFalse(averageSeries.containsKey("label"));
        }

        @Test
        void calculateSummary_ignoresWhitelistedSeriesWhenValuesAreNonNumeric() {
            RunResponse run = run("onemax", List.of(0, 1, 2), Map.of("fitness", new SeriesResponse<>(SeriesMode.ALL, List.of("bad", "data", "here"))), 10.0, 3);

            BatchSummaryResponse summary = service.calculateSummary(List.of(group(0, run)));
            AverageRunResponse average = summary.averageByProblem().get("onemax");

            assertEquals(List.of(0, 1, 2), average.evaluations());
            assertTrue(average.series().isEmpty());
        }

        @Test
        void calculateSummary_returnsEmptyBoxplotWhenBestFitnessIsMissing() {
            RunResponse run = run("onemax", List.of(0, 1, 2), Map.of("fitness", series(1, 2, 3)), 10.0, 3);

            BatchSummaryResponse summary = service.calculateSummary(List.of(group(0, run)));
            SeriesBoxPlotResponse boxPlot = summary.bestFitnessBoxPlotsByProblem().get("onemax");

            assertEquals(List.of(), boxPlot.evaluations());
            assertEquals(List.of(), boxPlot.boxplots());
        }

        @Test
        void calculateSummary_computesBestFitnessBoxplots() {
            RunResponse runA = run("onemax", List.of(0, 1, 2), Map.of("bestFitness", series(1, 3, 5)), 10.0, 3);
            RunResponse runB = run("onemax", List.of(0, 1, 2), Map.of("bestFitness", series(2, 4, 6)), 20.0, 3);

            BatchSummaryResponse summary = service.calculateSummary(List.of(group(0, runA), group(1, runB)));
            SeriesBoxPlotResponse boxPlot = summary.bestFitnessBoxPlotsByProblem().get("onemax");

            assertEquals(List.of(0, 1, 2), boxPlot.evaluations());
            assertEquals(List.of(1.0, 1.25, 1.5, 1.75, 2.0), boxPlot.boxplots().get(0));
            assertEquals(List.of(3.0, 3.25, 3.5, 3.75, 4.0), boxPlot.boxplots().get(1));
            assertEquals(List.of(5.0, 5.25, 5.5, 5.75, 6.0), boxPlot.boxplots().get(2));
        }

        @Test
        void calculateSummary_samplesBestFitnessBoxplotsWhenReferenceAxisIsLong() {
            List<Integer> evaluations = IntStream.range(0, 101).boxed().toList();
            List<Integer> values = IntStream.range(0, 101).boxed().toList();

            RunResponse run = run("onemax", evaluations, Map.of("bestFitness", seriesFromList(values)), 10.0, 101);

            BatchSummaryResponse summary = service.calculateSummary(List.of(group(0, run)));

            SeriesBoxPlotResponse boxPlot = summary.bestFitnessBoxPlotsByProblem().get("onemax");

            assertEquals(34, boxPlot.evaluations().size());
            assertEquals(0, boxPlot.evaluations().getFirst());
            assertEquals(3, boxPlot.evaluations().get(1));
            assertEquals(99, boxPlot.evaluations().getLast());
            assertEquals(List.of(0.0, 0.0, 0.0, 0.0, 0.0), boxPlot.boxplots().getFirst());
        }

        @Test
        void calculateSummary_emptyInputReturnsEmptySummary() {
            BatchSummaryResponse summary = service.calculateSummary(List.of());

            assertTrue(summary.averageByProblem().isEmpty());
            assertTrue(summary.bestFitnessBoxPlotsByProblem().isEmpty());
            assertTrue(summary.averageRunTimeByProblem().isEmpty());
        }
    }

    @Nested
    class RuntimeStudyPoint {

        @Test
        void toRuntimeStudyPoint_computesMeanAndFiveNumberSummary() {
            RunResponse a = run("onemax", List.of(0), Map.of(), 1.0, 10);
            RunResponse b = run("onemax", List.of(0), Map.of(), 1.0, 20);
            RunResponse c = run("onemax", List.of(0), Map.of(), 1.0, 30);

            RuntimeStudyPointResponse point = service.toRuntimeStudyPoint(10, List.of(group(0, a), group(1, b, c)));

            assertEquals(10, point.problemSize());
            assertEquals(20.0, point.meanEvaluationsToOptimum());
            assertEquals(List.of(10.0, 15.0, 20.0, 25.0, 30.0), point.boxPlot());
        }

        @Test
        void toRuntimeStudyPoint_returnsZeroAndEmptyBoxplotForNoRuns() {
            RuntimeStudyPointResponse point = service.toRuntimeStudyPoint(10, List.of(group(0)));

            assertEquals(10, point.problemSize());
            assertEquals(0.0, point.meanEvaluationsToOptimum());
            assertEquals(List.of(), point.boxPlot());
        }
    }

    private static RunGroupResponse group(int runIndex, RunResponse... runs) {
        return new RunGroupResponse(runIndex, 100L + runIndex, List.of(runs));
    }

    private static RunResponse run(
        String problemId,
        List<Integer> evaluations,
        Map<String, SeriesResponse<?>> series,
        double runtimeMs,
        int totalEvaluations
    ) {
        return new RunResponse("bitstring", problemId, evaluations, series, runtimeMs, totalEvaluations);
    }

    private static SeriesResponse<?> series(Number... values) {
        return new SeriesResponse<>(SeriesMode.ALL, List.of(values));
    }

    private static SeriesResponse<?> seriesFromList(List<? extends Number> values) {
        return new SeriesResponse<>(SeriesMode.ALL, List.copyOf(values));
    }
}