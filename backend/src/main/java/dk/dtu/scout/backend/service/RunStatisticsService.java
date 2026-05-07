package dk.dtu.scout.backend.service;

import dk.dtu.scout.backend.dto.run.AverageRunResponse;
import dk.dtu.scout.backend.dto.run.BatchSummaryResponse;
import dk.dtu.scout.backend.dto.run.RunGroupResponse;
import dk.dtu.scout.backend.dto.run.RunResponse;
import dk.dtu.scout.backend.dto.run.SeriesBoxPlotResponse;
import dk.dtu.scout.backend.dto.run.SeriesResponse;
import dk.dtu.scout.backend.dto.run.RuntimeStudyPointResponse;
import dk.dtu.scout.backend.util.StatisticsMath;
import dk.dtu.scout.backend.util.ViewMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provides statistical analysis of completed run results.
 * Used for normal run summaries and runtime-study points.
 * @author s230632
 */
@Service
public class RunStatisticsService {

    private static final int MAX_BOX_PLOTS = 50;
    private static final Set<String> AVERAGE_SERIES_WHITELIST = Set.of("fitness", "bestFitness");

    /**
     * Called after a batch of runs has completed, to compute statistics for the batch summary view.
     * @param batches completed run groups from RunExecutor
     * @return summary data for average curves, best-fitness boxplots, and average runtimes
     */
    public BatchSummaryResponse calculateSummary(List<RunGroupResponse> batches) {
        Map<String, List<RunResponse>> runsByProblem = groupRunsByProblem(batches);

        return new BatchSummaryResponse(
                computeAverageByProblem(runsByProblem),
                computeBestFitnessBoxPlotsByProblem(runsByProblem),
                computeAverageRuntimeByProblem(runsByProblem)
        );
    }

    /**
     * Groups run results by problem ID across all runtime groups.
     * This is needed because RunExecutor returns results grouped by runtime, while summaries are computed per problem.
     * @param batches completed run groups from RunExecutor
     * @return map from problem ID to all runs for that problem
     */
    private Map<String, List<RunResponse>> groupRunsByProblem(List<RunGroupResponse> batches) {
        Map<String, List<RunResponse>> runsByProblem = new LinkedHashMap<>();

        for (RunGroupResponse batch : batches) {
            for (RunResponse run : batch.runs()) {
                runsByProblem.computeIfAbsent(run.problemId(), key -> new ArrayList<>()).add(run);
            }
        }

        return runsByProblem;
    }

    /**
     * Computes one runtime-study point for a specific problem size.
     * The point is based on the distribution of final evaluations across repeated runs.
     * @param problemSize the problem size represented by this point
     * @param batches completed run groups for this problem size
     * @return runtime-study point containing mean and five-number summary of final evaluations
     */
    public RuntimeStudyPointResponse toRuntimeStudyPoint(int problemSize, List<RunGroupResponse> batches) {
        List<Double> totalEvaluations = new ArrayList<>();

        for (RunGroupResponse group : batches) {
            for (RunResponse run : group.runs()) {
                totalEvaluations.add((double) run.totalEvaluations());
            }
        }

        totalEvaluations.sort(Double::compareTo);

        return new RuntimeStudyPointResponse(
                problemSize,
                StatisticsMath.mean(totalEvaluations),
                StatisticsMath.fiveNumberSummary(totalEvaluations)
        );
    }

    /**
     * Computes averaged fitness curves for each problem.
     * @param runsByProblem map from problem ID to the runs for that problem
     * @return map from problem ID to the averaged run response
     */
    private Map<String, AverageRunResponse> computeAverageByProblem(Map<String, List<RunResponse>> runsByProblem) {
        Map<String, AverageRunResponse> result = new LinkedHashMap<>();

        for (Map.Entry<String, List<RunResponse>> entry : runsByProblem.entrySet()) {
            result.put(entry.getKey(), computeAverageRun(entry.getValue()));
        }

        return result;
    }

    /**
     * Computes best-fitness boxplot data for each problem.
     * @param runsByProblem map from problem ID to the runs for that problem
     * @return map from problem ID to sampled best-fitness boxplots
     */
    private Map<String, SeriesBoxPlotResponse> computeBestFitnessBoxPlotsByProblem(
            Map<String, List<RunResponse>> runsByProblem
    ) {
        Map<String, SeriesBoxPlotResponse> result = new LinkedHashMap<>();

        for (Map.Entry<String, List<RunResponse>> entry : runsByProblem.entrySet()) {
            result.put(entry.getKey(), computeBestFitnessBoxPlot(entry.getValue()));
        }

        return result;
    }

    /**
     * Computes average runtime in milliseconds for each problem.
     * @param runsByProblem map from problem ID to the runs for that problem
     * @return map from problem ID to average runtime in milliseconds
     */
    private Map<String, Double> computeAverageRuntimeByProblem(Map<String, List<RunResponse>> runsByProblem) {
        Map<String, Double> result = new LinkedHashMap<>();

        for (Map.Entry<String, List<RunResponse>> entry : runsByProblem.entrySet()) {
            List<Double> runtimes = entry.getValue().stream().map(RunResponse::runtimeMs).toList();
            result.put(entry.getKey(), StatisticsMath.mean(runtimes));
        }

        return result;
    }

    /**
     * Computes one averaged run from repeated runs of the same problem.
     * A reference run is used to provide the evaluation axis for averaging.
     * @param runs repeated runs for the same problem
     * @return averaged run response for the problem
     */
    private AverageRunResponse computeAverageRun(List<RunResponse> runs) {
        RunResponse referenceRun = findReferenceRun(runs);

        List<Integer> referenceEvaluations = new ArrayList<>(referenceRun.evaluations());
        Map<String, List<Double>> averageSeries = computeAverageSeries(runs, referenceEvaluations);

        return ViewMapper.toAverageRunResponse(referenceEvaluations, averageSeries);
    }

    /**
     * Computes average values for whitelisted numeric series at the reference evaluations.
     * @param runs repeated runs for the same problem
     * @param referenceEvaluations evaluation axis used for alignment
     * @return map from series name to averaged values
     */
    private Map<String, List<Double>> computeAverageSeries(List<RunResponse> runs, List<Integer> referenceEvaluations) {
        Map<String, List<Double>> result = new LinkedHashMap<>();

        Map<String, List<AlignedSeries>> seriesByName = collectAverageableSeries(runs);

        for (Map.Entry<String, List<AlignedSeries>> entry : seriesByName.entrySet()) {
            List<Double> averagedValues = averageSeriesAtReferenceEvaluations(entry.getValue(), referenceEvaluations);
            result.put(entry.getKey(), averagedValues);
        }

        return result;
    }

    /**
     * Extracts whitelisted numeric series from the runs.
     * Non-numeric series are ignored because they cannot be averaged.
     * @param runs repeated runs for the same problem
     * @return map from series name to aligned numeric series
     */
    private Map<String, List<AlignedSeries>> collectAverageableSeries(List<RunResponse> runs) {
        Map<String, List<AlignedSeries>> result = new LinkedHashMap<>();

        for (RunResponse run : runs) {
            for (String seriesName : run.series().keySet()) {
                if (!AVERAGE_SERIES_WHITELIST.contains(seriesName)) {
                    continue;
                }

                AlignedSeries aligned = extractAlignedSeries(run, seriesName);
                if (aligned != null) {
                    result.computeIfAbsent(seriesName, key -> new ArrayList<>()).add(aligned);
                }
            }
        }

        return result;
    }

    /**
     * Averages aligned series values at each reference evaluation.
     * If a run does not have a value exactly at the target evaluation, the most recent previous value is used.
     * @param alignedRuns aligned numeric series from repeated runs
     * @param referenceEvaluations evaluation axis used for averaging
     * @return averaged values at the reference evaluations
     */
    private List<Double> averageSeriesAtReferenceEvaluations(
            List<AlignedSeries> alignedRuns,
            List<Integer> referenceEvaluations
    ) {
        List<Double> averaged = new ArrayList<>(referenceEvaluations.size());

        for (Integer targetEvaluation : referenceEvaluations) {
            List<Double> values = new ArrayList<>();

            for (AlignedSeries aligned : alignedRuns) {
                values.add(valueAtEvaluation(aligned.evaluations(), aligned.values(), targetEvaluation));
            }

            averaged.add(StatisticsMath.mean(values));
        }

        return averaged;
    }

    /**
     * Computes sampled boxplots for the bestFitness series across repeated runs.
     * @param runs repeated runs for the same problem
     * @return sampled best-fitness boxplot response
     */
    private SeriesBoxPlotResponse computeBestFitnessBoxPlot(List<RunResponse> runs) {
        RunResponse referenceRun = findReferenceRun(runs);
        List<Integer> referenceEvaluations = new ArrayList<>(referenceRun.evaluations());

        List<AlignedSeries> bestFitnessRuns = extractBestFitnessSeries(runs);

        if (bestFitnessRuns.isEmpty()) {
            return new SeriesBoxPlotResponse(List.of(), List.of());
        }

        return sampleBestFitnessBoxPlots(referenceEvaluations, bestFitnessRuns);
    }

    /**
     * Extracts the bestFitness series from all runs where it is available.
     * @param runs repeated runs for the same problem
     * @return aligned bestFitness series
     */
    private List<AlignedSeries> extractBestFitnessSeries(List<RunResponse> runs) {
        List<AlignedSeries> result = new ArrayList<>();

        for (RunResponse run : runs) {
            AlignedSeries aligned = extractAlignedSeries(run, "bestFitness");
            if (aligned != null) {
                result.add(aligned);
            }
        }

        return result;
    }

    /**
     * Samples evaluation positions and computes five-number summaries for bestFitness.
     * @param referenceEvaluations evaluation axis used for sampling
     * @param bestFitnessRuns aligned bestFitness series from repeated runs
     * @return sampled boxplot response
     */
    private SeriesBoxPlotResponse sampleBestFitnessBoxPlots(
            List<Integer> referenceEvaluations,
            List<AlignedSeries> bestFitnessRuns
    ) {
        int step = Math.max(1, (int) Math.ceil((double) referenceEvaluations.size() / MAX_BOX_PLOTS));

        List<Integer> sampledEvaluations = new ArrayList<>();
        List<List<Double>> boxplots = new ArrayList<>();

        for (int i = 0; i < referenceEvaluations.size(); i += step) {
            int targetEvaluation = referenceEvaluations.get(i);
            List<Double> valuesAtEvaluation = valuesAtEvaluation(bestFitnessRuns, targetEvaluation);

            valuesAtEvaluation.sort(Double::compareTo);

            sampledEvaluations.add(targetEvaluation);
            boxplots.add(StatisticsMath.fiveNumberSummary(valuesAtEvaluation));
        }

        return new SeriesBoxPlotResponse(sampledEvaluations, boxplots);
    }

    /**
     * Collects values from aligned runs at a single target evaluation.
     * @param alignedRuns aligned numeric series from repeated runs
     * @param targetEvaluation evaluation at which to collect values
     * @return values at the target evaluation
     */
    private List<Double> valuesAtEvaluation(List<AlignedSeries> alignedRuns, int targetEvaluation) {
        List<Double> values = new ArrayList<>();

        for (AlignedSeries aligned : alignedRuns) {
            values.add(valueAtEvaluation(aligned.evaluations(), aligned.values(), targetEvaluation));
        }

        return values;
    }

    /**
     * Selects the run with the longest evaluation axis as the reference run.
     * @param runs repeated runs for the same problem
     * @return run with the most logged evaluation points
     */
    private RunResponse findReferenceRun(List<RunResponse> runs) {
        return runs.stream()
                .max(Comparator.comparingInt(run -> run.evaluations().size()))
                .orElseThrow();
    }

    /**
     * Extracts a numeric series and aligns it with the run's evaluation list.
     * @param run run response containing logged series
     * @param seriesName name of the series to extract
     * @return aligned numeric series, or null if the series is missing or non-numeric
     */
    private AlignedSeries extractAlignedSeries(RunResponse run, String seriesName) {
        SeriesResponse<?> response = run.series().get(seriesName);
        if (response == null) {
            return null;
        }

        List<Double> numericValues = toDoubleList(response.values());
        if (numericValues.isEmpty()) {
            return null;
        }

        int usableLength = Math.min(run.evaluations().size(), numericValues.size());

        return new AlignedSeries(
                new ArrayList<>(run.evaluations().subList(0, usableLength)),
                new ArrayList<>(numericValues.subList(0, usableLength))
        );
    }

    /**
     * Converts raw series values to doubles.
     * @param rawValues raw values from a series response
     * @return double values, or an empty list if any value is non-numeric
     */
    private List<Double> toDoubleList(List<?> rawValues) {
        List<Double> result = new ArrayList<>();

        for (Object value : rawValues) {
            if (!(value instanceof Number number)) {
                return List.of();
            }

            result.add(number.doubleValue());
        }

        return result;
    }

    /**
     * Returns the value at the requested evaluation.
     * If the exact evaluation does not exist, the latest previous value is used.
     * @param evaluations evaluation axis for the series
     * @param values numeric values aligned with the evaluations
     * @param targetEvaluation evaluation to look up
     * @return value at the target evaluation
     */
    private Double valueAtEvaluation(List<Integer> evaluations, List<Double> values, int targetEvaluation) {
        int usableLength = Math.min(evaluations.size(), values.size());

        if (targetEvaluation <= evaluations.getFirst()) {
            return values.getFirst();
        }

        for (int i = 1; i < usableLength; i++) {
            if (evaluations.get(i) > targetEvaluation) {
                return values.get(i - 1);
            }
        }

        return values.get(usableLength - 1);
    }

    /**
     * Numeric series paired with its evaluation axis.
     * @param evaluations evaluation axis for the series
     * @param values numeric values aligned with the evaluations
     */
    private record AlignedSeries(List<Integer> evaluations, List<Double> values) {
    }
}