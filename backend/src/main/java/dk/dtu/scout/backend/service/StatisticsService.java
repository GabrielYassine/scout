package dk.dtu.scout.backend.service;

import dk.dtu.scout.backend.dto.run.AverageRunResponse;
import dk.dtu.scout.backend.dto.run.BatchSummaryResponse;
import dk.dtu.scout.backend.dto.run.RunGroupResponse;
import dk.dtu.scout.backend.dto.run.RunResponse;
import dk.dtu.scout.backend.dto.run.RuntimeStats;
import dk.dtu.scout.backend.dto.series.SeriesResponse;
import dk.dtu.scout.backend.util.ViewMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatisticsService {

    public BatchSummaryResponse calculateSummary(List<RunGroupResponse> batches) {
        Map<String, List<Double>> runtimesByProblem = new HashMap<>();
        Map<String, List<Integer>> finalEvaluationsByProblem = new HashMap<>();
        Map<String, List<RunResponse>> runsByProblem = new HashMap<>();

        for (RunGroupResponse batch : batches) {
            for (RunResponse run : batch.runs()) {
                runtimesByProblem.computeIfAbsent(run.problemId(), k -> new ArrayList<>()).add(run.runtimeMs());
                finalEvaluationsByProblem.computeIfAbsent(run.problemId(), k -> new ArrayList<>()).add(run.finalEvaluations());
                runsByProblem.computeIfAbsent(run.problemId(), k -> new ArrayList<>()).add(run);
            }
        }

        Map<String, RuntimeStats> statsByProblem = new HashMap<>();
        for (Map.Entry<String, List<Double>> entry : runtimesByProblem.entrySet()) {
            String problemId = entry.getKey();
            List<Double> runtimes = entry.getValue();
            List<Integer> finalEvals = finalEvaluationsByProblem.get(problemId);
            statsByProblem.put(problemId, computeStats(runtimes, finalEvals));
        }

        Map<String, AverageRunResponse> averageByProblem = new HashMap<>();
        for (Map.Entry<String, List<RunResponse>> entry : runsByProblem.entrySet()) {
            averageByProblem.put(entry.getKey(), computeAverageRun(entry.getValue()));
        }

        return ViewMapper.toBatchSummaryResponse(statsByProblem, averageByProblem);
    }

    private AverageRunResponse computeAverageRun(List<RunResponse> runs) {
        if (runs == null || runs.isEmpty()) {
            return ViewMapper.toAverageRunResponse(List.of(), List.of(), Map.of());
        }

        RunResponse referenceRun = runs.stream()
                .max((a, b) -> Integer.compare(
                        a.evaluations() != null ? a.evaluations().size() : 0,
                        b.evaluations() != null ? b.evaluations().size() : 0
                ))
                .orElse(runs.get(0));

        List<Integer> referenceIterations = referenceRun.iterations() != null ? new ArrayList<>(referenceRun.iterations()) : List.of();

        List<Integer> referenceEvaluations = referenceRun.evaluations() != null ? new ArrayList<>(referenceRun.evaluations()) : List.of();

        Map<String, List<Double>> averageSeries = computeAverageSeries(runs);

        return ViewMapper.toAverageRunResponse(referenceIterations, referenceEvaluations, averageSeries);
    }

    private Map<String, List<Double>> computeAverageSeries(List<RunResponse> runs) {
        Map<String, List<Double>> result = new LinkedHashMap<>();
        if (runs == null || runs.isEmpty()) {
            return result;
        }

        Map<String, List<List<Double>>> seriesValuesByName = new LinkedHashMap<>();

        for (RunResponse run : runs) {
            Map<String, SeriesResponse<?>> series = run.series();
            if (series == null) continue;

            for (Map.Entry<String, SeriesResponse<?>> entry : series.entrySet()) {
                String seriesName = entry.getKey();
                SeriesResponse<?> response = entry.getValue();
                if (response == null) continue;
                List<?> rawValues = response.values();

                List<Double> numericValues = toDoubleList(rawValues);
                if (numericValues.isEmpty()) continue;

                seriesValuesByName
                        .computeIfAbsent(seriesName, k -> new ArrayList<>())
                        .add(numericValues);
            }
        }

        for (Map.Entry<String, List<List<Double>>> entry : seriesValuesByName.entrySet()) {
            String seriesName = entry.getKey();
            List<List<Double>> allRunsForSeries = entry.getValue();

            int maxLength = allRunsForSeries.stream()
                    .mapToInt(List::size)
                    .max()
                    .orElse(0);

            if (maxLength == 0) continue;

            List<Double> averaged = new ArrayList<>(maxLength);

            for (int i = 0; i < maxLength; i++) {
                double sum = 0.0;

                for (List<Double> runSeries : allRunsForSeries) {
                    double value;
                    if (i < runSeries.size()) {
                        value = runSeries.get(i);
                    } else {
                        value = runSeries.get(runSeries.size() - 1);
                    }
                    sum += value;
                }

                averaged.add(sum / allRunsForSeries.size());
            }

            result.put(seriesName, averaged);
        }

        return result;
    }

    private List<Double> toDoubleList(List<?> rawValues) {
        List<Double> result = new ArrayList<>();
        if (rawValues == null) return result;

        for (Object value : rawValues) {
            if (!(value instanceof Number number)) {
                return List.of();
            }
            result.add(number.doubleValue());
        }

        return result;
    }



    public RuntimeStats computeStats(List<Double> values, List<Integer> finalEvaluations) {
        int n = values.size();
        if (n == 0) {
            return ViewMapper.toRuntimeStats(0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        }
        double finalEvaluationsMedian = calculateMedian(finalEvaluations);
        return ViewMapper.toRuntimeStats(n, 0.0, 0.0, 0.0, 0.0, 0.0, finalEvaluationsMedian);
    }

    private double calculateMedian(List<Integer> values) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }

        List<Integer> sorted = new ArrayList<>(values);
        sorted.sort(Integer::compareTo);

        int size = sorted.size();
        if (size % 2 == 0) {
            return (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2.0;
        } else {
            return sorted.get(size / 2);
        }
    }
}