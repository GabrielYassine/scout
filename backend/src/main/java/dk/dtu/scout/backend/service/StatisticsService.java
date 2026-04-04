package dk.dtu.scout.backend.service;

import dk.dtu.scout.backend.dto.run.AverageRunResponse;
import dk.dtu.scout.backend.dto.run.BatchSummaryResponse;
import dk.dtu.scout.backend.dto.run.RunGroupResponse;
import dk.dtu.scout.backend.dto.run.RunResponse;
import dk.dtu.scout.backend.dto.run.SeriesBoxPlotResponse;
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
        Map<String, List<Integer>> finalEvaluationsByProblem = new HashMap<>();
        Map<String, List<RunResponse>> runsByProblem = new HashMap<>();

        for (RunGroupResponse batch : batches) {
            for (RunResponse run : batch.runs()) {
                finalEvaluationsByProblem.computeIfAbsent(run.problemId(), k -> new ArrayList<>()).add(run.finalEvaluations());
                runsByProblem.computeIfAbsent(run.problemId(), k -> new ArrayList<>()).add(run);
            }
        }


        Map<String, AverageRunResponse> averageByProblem = new HashMap<>();
        for (Map.Entry<String, List<RunResponse>> entry : runsByProblem.entrySet()) {
            averageByProblem.put(entry.getKey(), computeAverageRun(entry.getValue()));
        }

        Map<String, SeriesBoxPlotResponse> bestFitnessBoxPlotsByProblem = new HashMap<>();
        for (Map.Entry<String, List<RunResponse>> entry : runsByProblem.entrySet()) {
            bestFitnessBoxPlotsByProblem.put(entry.getKey(), computeBestFitnessBoxPlot(entry.getValue()));
        }

        return new BatchSummaryResponse(averageByProblem, bestFitnessBoxPlotsByProblem);
    }

    private AverageRunResponse computeAverageRun(List<RunResponse> runs) {
        if (runs == null || runs.isEmpty()) {
            return ViewMapper.toAverageRunResponse(List.of(), List.of(), Map.of());
        }

        RunResponse referenceRun = findReferenceRun(runs);

        List<Integer> referenceIterations = referenceRun.iterations() != null ? new ArrayList<>(referenceRun.iterations()) : List.of();

        List<Integer> referenceEvaluations = referenceRun.evaluations() != null ? new ArrayList<>(referenceRun.evaluations()) : List.of();

        Map<String, List<Double>> averageSeries = computeAverageSeries(runs, referenceEvaluations);

        return ViewMapper.toAverageRunResponse(referenceIterations, referenceEvaluations, averageSeries);
    }

    private Map<String, List<Double>> computeAverageSeries(List<RunResponse> runs, List<Integer> referenceEvaluations) {
        Map<String, List<Double>> result = new LinkedHashMap<>();
        if (runs == null || runs.isEmpty() || referenceEvaluations == null || referenceEvaluations.isEmpty()) {
            return result;
        }

        Map<String, List<AlignedSeries>> seriesValuesByName = new LinkedHashMap<>();

        for (RunResponse run : runs) {
            Map<String, SeriesResponse<?>> series = run.series();
            List<Integer> runEvaluations = run.evaluations();

            if (series == null || runEvaluations == null || runEvaluations.isEmpty()) {
                continue;
            }

            for (Map.Entry<String, SeriesResponse<?>> entry : series.entrySet()) {
                String seriesName = entry.getKey();
                SeriesResponse<?> response = entry.getValue();
                if (response == null) continue;

                List<Double> numericValues = toDoubleList(response.values());
                if (numericValues.isEmpty()) continue;

                int usableLength = Math.min(runEvaluations.size(), numericValues.size());
                if (usableLength == 0) continue;

                seriesValuesByName
                        .computeIfAbsent(seriesName, k -> new ArrayList<>())
                        .add(new AlignedSeries(
                                new ArrayList<>(runEvaluations.subList(0, usableLength)),
                                new ArrayList<>(numericValues.subList(0, usableLength))
                        ));
            }
        }

        for (Map.Entry<String, List<AlignedSeries>> entry : seriesValuesByName.entrySet()) {
            String seriesName = entry.getKey();
            List<AlignedSeries> alignedRuns = entry.getValue();

            if (alignedRuns.isEmpty()) continue;

            List<Double> averaged = new ArrayList<>(referenceEvaluations.size());

            for (Integer targetEvaluation : referenceEvaluations) {
                double sum = 0.0;
                int count = 0;

                for (AlignedSeries aligned : alignedRuns) {
                    Double value = valueAtEvaluation(
                            aligned.evaluations(),
                            aligned.values(),
                            targetEvaluation
                    );
                    if (value != null) {
                        sum += value;
                        count++;
                    }
                }

                if (count > 0) {
                    averaged.add(sum / count);
                }
            }

            if (!averaged.isEmpty()) {
                result.put(seriesName, averaged);
            }
        }

        return result;
    }

    private SeriesBoxPlotResponse computeBestFitnessBoxPlot(List<RunResponse> runs) {
        if (runs == null || runs.isEmpty()) {
            return new SeriesBoxPlotResponse(List.of(), List.of());
        }

        RunResponse referenceRun = findReferenceRun(runs);
        List<Integer> referenceEvaluations =
                referenceRun.evaluations() != null ? new ArrayList<>(referenceRun.evaluations()) : List.of();

        List<List<Double>> allRunsBestFitness = new ArrayList<>();

        for (RunResponse run : runs) {
            Map<String, SeriesResponse<?>> series = run.series();
            if (series == null) continue;

            SeriesResponse<?> response = series.get("bestFitness");
            if (response == null) continue;

            List<Double> numericValues = toDoubleList(response.values());
            if (numericValues.isEmpty()) continue;

            allRunsBestFitness.add(numericValues);
        }

        if (allRunsBestFitness.isEmpty()) {
            return new SeriesBoxPlotResponse(List.of(), List.of());
        }

        int maxLength = allRunsBestFitness.stream()
                .mapToInt(List::size)
                .max()
                .orElse(0);

        if (maxLength == 0) {
            return new SeriesBoxPlotResponse(List.of(), List.of());
        }

        List<List<Double>> boxplots = new ArrayList<>(maxLength);

        for (int i = 0; i < maxLength; i++) {
            List<Double> valuesAtIndex = new ArrayList<>();

            for (List<Double> runSeries : allRunsBestFitness) {
                double value = i < runSeries.size()
                        ? runSeries.get(i)
                        : runSeries.get(runSeries.size() - 1);
                valuesAtIndex.add(value);
            }

            valuesAtIndex.sort(Double::compareTo);

            boxplots.add(List.of(
                    valuesAtIndex.getFirst(),
                    percentile(valuesAtIndex, 25),
                    percentile(valuesAtIndex, 50),
                    percentile(valuesAtIndex, 75),
                    valuesAtIndex.getLast()
            ));
        }

        List<Integer> evaluations = referenceEvaluations.size() >= maxLength
                ? new ArrayList<>(referenceEvaluations.subList(0, maxLength))
                : padEvaluations(referenceEvaluations, maxLength);

        return new SeriesBoxPlotResponse(evaluations, boxplots);
    }

    private RunResponse findReferenceRun(List<RunResponse> runs) {
        return runs.stream()
                .max((a, b) -> Integer.compare(
                        a.evaluations() != null ? a.evaluations().size() : 0,
                        b.evaluations() != null ? b.evaluations().size() : 0
                ))
                .orElse(runs.getFirst());
    }

    private List<Integer> padEvaluations(List<Integer> evaluations, int targetLength) {
        if (targetLength <= 0) return List.of();
        if (evaluations == null || evaluations.isEmpty()) {
            List<Integer> fallback = new ArrayList<>(targetLength);
            for (int i = 0; i < targetLength; i++) {
                fallback.add(i);
            }
            return fallback;
        }

        List<Integer> out = new ArrayList<>(evaluations);
        int last = evaluations.getLast();
        while (out.size() < targetLength) {
            out.add(last);
        }
        return out;
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

    private double percentile(List<Double> sorted, double p) {
        if (sorted == null || sorted.isEmpty()) return 0.0;
        if (sorted.size() == 1) return sorted.getFirst();

        double index = (p / 100.0) * (sorted.size() - 1);
        int lower = (int) Math.floor(index);
        int upper = (int) Math.ceil(index);

        if (lower == upper) {
            return sorted.get(lower);
        }

        double fraction = index - lower;
        return sorted.get(lower) + fraction * (sorted.get(upper) - sorted.get(lower));
    }
    private record AlignedSeries(List<Integer> evaluations, List<Double> values) {}
    private Double valueAtEvaluation(List<Integer> evaluations, List<Double> values, int targetEvaluation) {
        if (evaluations == null || values == null || evaluations.isEmpty() || values.isEmpty()) {
            return null;
        }

        int usableLength = Math.min(evaluations.size(), values.size());
        if (usableLength == 0) {
            return null;
        }

        if (targetEvaluation <= evaluations.get(0)) {
            return values.get(0);
        }

        for (int i = 1; i < usableLength; i++) {
            if (evaluations.get(i) > targetEvaluation) {
                return values.get(i - 1);
            }
        }

        return values.get(usableLength - 1);
    }
}