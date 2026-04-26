package dk.dtu.scout.backend.service;

import dk.dtu.scout.backend.dto.run.AverageRunResponse;
import dk.dtu.scout.backend.dto.run.BatchSummaryResponse;
import dk.dtu.scout.backend.dto.run.RunGroupResponse;
import dk.dtu.scout.backend.dto.run.RunResponse;
import dk.dtu.scout.backend.dto.run.SeriesBoxPlotResponse;
import dk.dtu.scout.backend.dto.series.SeriesResponse;
import dk.dtu.scout.backend.dto.study.RuntimeStudyPointResponse;
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
 * Provides statistical analysis of run results for summary views and runtime studies.
 * @author Ahmed
 */
@Service
public class RunStatisticsService {

    private static final int MAX_BOX_PLOTS = 50;
    private static final Set<String> AVERAGE_SERIES_WHITELIST = Set.of("fitness", "bestFitness");

    /**
     * Called after a batch of runs has completed, to compute statistics for the batch summary view.
     * @param batches the data from a whole run, containing runresponses from all runtimes and problems, grouped by batch.
     * @return a BatchSummaryResponse containing the computed statistics for each problem, to be used in the batch summary view.
     */
    public BatchSummaryResponse calculateSummary(List<RunGroupResponse> batches) {
        Map<String, List<RunResponse>> runsByProblem = groupRunsByProblem(batches);

        return new BatchSummaryResponse(
            computeAverageByProblem(runsByProblem),
            computeBestFitnessBoxPlotsByProblem(runsByProblem),
            computeAverageRuntimeByProblem(runsByProblem)
        );
    }

    public RuntimeStudyPointResponse toRuntimeStudyPoint(int problemSize, List<RunGroupResponse> batches) {
        List<Double> finalEvaluations = new ArrayList<>();

        for (RunGroupResponse group : batches) {
            for (RunResponse run : group.runs()) {
                finalEvaluations.add((double) run.finalEvaluations());
            }
        }

        finalEvaluations.sort(Double::compareTo);

        return new RuntimeStudyPointResponse(
                problemSize,
                StatisticsMath.mean(finalEvaluations),
                StatisticsMath.fiveNumberSummary(finalEvaluations)
        );
    }

    private Map<String, List<RunResponse>> groupRunsByProblem(List<RunGroupResponse> batches) {
        Map<String, List<RunResponse>> runsByProblem = new LinkedHashMap<>();

        if (batches == null) {
            return runsByProblem;
        }

        for (RunGroupResponse batch : batches) {
            if (batch == null || batch.runs() == null) {
                continue;
            }

            for (RunResponse run : batch.runs()) {
                runsByProblem.computeIfAbsent(run.problemId(), key -> new ArrayList<>()).add(run);
            }
        }

        return runsByProblem;
    }

    private Map<String, AverageRunResponse> computeAverageByProblem(Map<String, List<RunResponse>> runsByProblem) {
        Map<String, AverageRunResponse> result = new LinkedHashMap<>();

        for (Map.Entry<String, List<RunResponse>> entry : runsByProblem.entrySet()) {
            result.put(entry.getKey(), computeAverageRun(entry.getValue()));
        }

        return result;
    }

    private Map<String, SeriesBoxPlotResponse> computeBestFitnessBoxPlotsByProblem(Map<String, List<RunResponse>> runsByProblem) {
        Map<String, SeriesBoxPlotResponse> result = new LinkedHashMap<>();

        for (Map.Entry<String, List<RunResponse>> entry : runsByProblem.entrySet()) {
            result.put(entry.getKey(), computeBestFitnessBoxPlot(entry.getValue()));
        }

        return result;
    }

    private Map<String, Double> computeAverageRuntimeByProblem(Map<String, List<RunResponse>> runsByProblem) {
        Map<String, Double> result = new LinkedHashMap<>();

        for (Map.Entry<String, List<RunResponse>> entry : runsByProblem.entrySet()) {
            List<Double> runtimes = entry.getValue().stream()
                    .map(RunResponse::runtimeMs)
                    .toList();

            result.put(entry.getKey(), StatisticsMath.mean(runtimes));
        }

        return result;
    }

    private AverageRunResponse computeAverageRun(List<RunResponse> runs) {
        if (runs == null || runs.isEmpty()) {
            return ViewMapper.toAverageRunResponse(List.of(), List.of(), Map.of());
        }

        RunResponse referenceRun = findReferenceRun(runs);

        List<Integer> referenceIterations = safeIntegerList(referenceRun.iterations());
        List<Integer> referenceEvaluations = safeIntegerList(referenceRun.evaluations());
        Map<String, List<Double>> averageSeries = computeAverageSeries(runs, referenceEvaluations);

        return ViewMapper.toAverageRunResponse(referenceIterations, referenceEvaluations, averageSeries);
    }

    private Map<String, List<Double>> computeAverageSeries(List<RunResponse> runs, List<Integer> referenceEvaluations) {
        Map<String, List<Double>> result = new LinkedHashMap<>();

        if (runs == null || runs.isEmpty() || referenceEvaluations == null || referenceEvaluations.isEmpty()) {
            return result;
        }

        Map<String, List<AlignedSeries>> seriesByName = collectAverageableSeries(runs);

        for (Map.Entry<String, List<AlignedSeries>> entry : seriesByName.entrySet()) {
            List<Double> averagedValues = averageSeriesAtReferenceEvaluations(
                    entry.getValue(),
                    referenceEvaluations
            );

            if (!averagedValues.isEmpty()) {
                result.put(entry.getKey(), averagedValues);
            }
        }

        return result;
    }

    private Map<String, List<AlignedSeries>> collectAverageableSeries(List<RunResponse> runs) {
        Map<String, List<AlignedSeries>> result = new LinkedHashMap<>();

        for (RunResponse run : runs) {
            if (run.series() == null) {
                continue;
            }

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

    private List<Double> averageSeriesAtReferenceEvaluations(List<AlignedSeries> alignedRuns, List<Integer> referenceEvaluations) {
        List<Double> averaged = new ArrayList<>(referenceEvaluations.size());

        for (Integer targetEvaluation : referenceEvaluations) {
            List<Double> values = new ArrayList<>();

            for (AlignedSeries aligned : alignedRuns) {
                Double value = valueAtEvaluation(aligned.evaluations(), aligned.values(), targetEvaluation);
                if (value != null) {
                    values.add(value);
                }
            }

            if (!values.isEmpty()) {
                averaged.add(StatisticsMath.mean(values));
            }
        }

        return averaged;
    }

    private SeriesBoxPlotResponse computeBestFitnessBoxPlot(List<RunResponse> runs) {
        if (runs == null || runs.isEmpty()) {
            return new SeriesBoxPlotResponse(List.of(), List.of());
        }

        RunResponse referenceRun = findReferenceRun(runs);
        List<Integer> referenceEvaluations = safeIntegerList(referenceRun.evaluations());

        if (referenceEvaluations.isEmpty()) {
            return new SeriesBoxPlotResponse(List.of(), List.of());
        }

        List<AlignedSeries> bestFitnessRuns = extractBestFitnessSeries(runs);

        if (bestFitnessRuns.isEmpty()) {
            return new SeriesBoxPlotResponse(List.of(), List.of());
        }

        return sampleBestFitnessBoxPlots(referenceEvaluations, bestFitnessRuns);
    }

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

    private SeriesBoxPlotResponse sampleBestFitnessBoxPlots(List<Integer> referenceEvaluations, List<AlignedSeries> bestFitnessRuns) {
        int step = Math.max(1, (int) Math.ceil((double) referenceEvaluations.size() / MAX_BOX_PLOTS));

        List<Integer> sampledEvaluations = new ArrayList<>();
        List<List<Double>> boxplots = new ArrayList<>();

        for (int i = 0; i < referenceEvaluations.size(); i += step) {
            int targetEvaluation = referenceEvaluations.get(i);
            List<Double> valuesAtEvaluation = valuesAtEvaluation(bestFitnessRuns, targetEvaluation);

            if (valuesAtEvaluation.isEmpty()) {
                continue;
            }

            valuesAtEvaluation.sort(Double::compareTo);

            sampledEvaluations.add(targetEvaluation);
            boxplots.add(StatisticsMath.fiveNumberSummary(valuesAtEvaluation));
        }

        return new SeriesBoxPlotResponse(sampledEvaluations, boxplots);
    }

    private List<Double> valuesAtEvaluation(List<AlignedSeries> alignedRuns, int targetEvaluation) {
        List<Double> values = new ArrayList<>();

        for (AlignedSeries aligned : alignedRuns) {
            Double value = valueAtEvaluation(aligned.evaluations(), aligned.values(), targetEvaluation);
            if (value != null) {
                values.add(value);
            }
        }

        return values;
    }

    private RunResponse findReferenceRun(List<RunResponse> runs) {
        return runs.stream().max(Comparator.comparingInt(run -> run.evaluations() != null ? run.evaluations().size() : 0)).orElse(runs.getFirst());
    }

    private AlignedSeries extractAlignedSeries(RunResponse run, String seriesName) {
        if (run == null || run.series() == null || run.evaluations() == null || run.evaluations().isEmpty()) {
            return null;
        }

        SeriesResponse<?> response = run.series().get(seriesName);
        if (response == null) {
            return null;
        }

        List<Double> numericValues = toDoubleList(response.values());
        if (numericValues.isEmpty()) {
            return null;
        }

        int usableLength = Math.min(run.evaluations().size(), numericValues.size());
        if (usableLength == 0) {
            return null;
        }

        return new AlignedSeries(
                new ArrayList<>(run.evaluations().subList(0, usableLength)),
                new ArrayList<>(numericValues.subList(0, usableLength))
        );
    }

    private List<Double> toDoubleList(List<?> rawValues) {
        List<Double> result = new ArrayList<>();

        if (rawValues == null) {
            return result;
        }

        for (Object value : rawValues) {
            if (!(value instanceof Number number)) {
                return List.of();
            }

            result.add(number.doubleValue());
        }

        return result;
    }

    private Double valueAtEvaluation(List<Integer> evaluations, List<Double> values, int targetEvaluation) {
        if (evaluations == null || values == null || evaluations.isEmpty() || values.isEmpty()) {
            return null;
        }

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

    private List<Integer> safeIntegerList(List<Integer> values) {
        return values != null ? new ArrayList<>(values) : List.of();
    }

    private record AlignedSeries(List<Integer> evaluations, List<Double> values) {
    }
}