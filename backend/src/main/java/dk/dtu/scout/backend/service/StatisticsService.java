package dk.dtu.scout.backend.service;

import dk.dtu.scout.backend.dto.run.*;
import dk.dtu.scout.backend.dto.series.SeriesResponse;
import dk.dtu.scout.backend.dto.study.RuntimeStudyPointResponse;
import dk.dtu.scout.backend.dto.stats.SeriesPoint;
import dk.dtu.scout.backend.dto.stats.SeriesWindowStatsRequest;
import dk.dtu.scout.backend.dto.stats.SeriesWindowStatsResponse;
import dk.dtu.scout.backend.exception.BadRequestException;
import dk.dtu.scout.backend.util.ViewMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatisticsService {
    private static final int MAX_BOX_PLOTS = 50;
    private static final double TREND_THRESHOLD = 0.0001;

    /**
     * Builds the normal run summary used by the regular run page.
     *
     *  Includes:
     *   average series per problem
     *   boxplots for bestFitness
     *   average runtime per problem
     */
    public BatchSummaryResponse calculateSummary(List<RunGroupResponse> batches) {
        Map<String, List<RunResponse>> runsByProblem = groupRunsByProblem(batches);

        Map<String, AverageRunResponse> averageByProblem = computeAverageByProblem(runsByProblem);
        Map<String, SeriesBoxPlotResponse> bestFitnessBoxPlotsByProblem = computeBestFitnessBoxPlotsByProblem(runsByProblem);
        Map<String, Double> averageRunTimeByProblem = computeAverageRuntimeByProblem(runsByProblem);

        return new BatchSummaryResponse(averageByProblem, bestFitnessBoxPlotsByProblem,averageRunTimeByProblem);
    }

    /**
     * Groups all runs by problem id.
     */
    private Map<String, List<RunResponse>> groupRunsByProblem(List<RunGroupResponse> batches) {
        Map<String, List<RunResponse>> runsByProblem = new LinkedHashMap<>();

        for (RunGroupResponse batch : batches) {
            for (RunResponse run : batch.runs()) {
                runsByProblem
                        .computeIfAbsent(run.problemId(), k -> new ArrayList<>())
                        .add(run);
            }
        }

        return runsByProblem;
    }
    /**
     * Computes average series data for every problem.
     */
    private Map<String, AverageRunResponse> computeAverageByProblem(Map<String, List<RunResponse>> runsByProblem) {
        Map<String, AverageRunResponse> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<RunResponse>> entry : runsByProblem.entrySet()) {
            result.put(entry.getKey(), computeAverageRun(entry.getValue()));
        }
        return result;
    }

    /**
     * Computes bestFitness boxplots for every problem.
     */
    private Map<String, SeriesBoxPlotResponse> computeBestFitnessBoxPlotsByProblem(Map<String, List<RunResponse>> runsByProblem) {
        Map<String, SeriesBoxPlotResponse> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<RunResponse>> entry : runsByProblem.entrySet()) {
            result.put(entry.getKey(), computeBestFitnessBoxPlot(entry.getValue()));
        }
        return result;
    }
    /**
     * Builds one runtime-study point for one problem size.
     */
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
            if (series == null) continue;

            for (String seriesName : series.keySet()) {
                AlignedSeries aligned = extractAlignedSeries(run, seriesName);
                if (aligned == null) continue;

                seriesValuesByName
                        .computeIfAbsent(seriesName, k -> new ArrayList<>())
                        .add(aligned);
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
        List<Integer> referenceEvaluations = referenceRun.evaluations() != null ? new ArrayList<>(referenceRun.evaluations()) : List.of();

        if (referenceEvaluations.isEmpty()) {
            return new SeriesBoxPlotResponse(List.of(), List.of());
        }

        Map<RunResponse, AlignedSeries> bestFitnessByRun = new LinkedHashMap<>();

        for (RunResponse run : runs) {
            AlignedSeries aligned = extractAlignedSeries(run, "bestFitness");
            if (aligned != null) {
                bestFitnessByRun.put(run, aligned);
            }
        }

        if (bestFitnessByRun.isEmpty()) {
            return new SeriesBoxPlotResponse(List.of(), List.of());
        }

        int step = Math.max(1, (int) Math.ceil((double) referenceEvaluations.size() / MAX_BOX_PLOTS));

        List<Integer> sampledEvaluations = new ArrayList<>();
        List<List<Double>> boxplots = new ArrayList<>();

        for (int i = 0; i < referenceEvaluations.size(); i += step) {
            int targetEvaluation = referenceEvaluations.get(i);
            List<Double> valuesAtEvaluation = new ArrayList<>();

            for (AlignedSeries aligned : bestFitnessByRun.values()) {
                Double value = valueAtEvaluation(
                        aligned.evaluations(),
                        aligned.values(),
                        targetEvaluation
                );
                if (value != null) {
                    valuesAtEvaluation.add(value);
                }
            }

            if (valuesAtEvaluation.isEmpty()) {
                continue;
            }

            valuesAtEvaluation.sort(Double::compareTo);

            sampledEvaluations.add(targetEvaluation);
            boxplots.add(List.of(
                    valuesAtEvaluation.getFirst(),
                    percentile(valuesAtEvaluation, 25),
                    percentile(valuesAtEvaluation, 50),
                    percentile(valuesAtEvaluation, 75),
                    valuesAtEvaluation.getLast()
            ));
        }

        return new SeriesBoxPlotResponse(sampledEvaluations, boxplots);
    }

    private Map<String, Double> computeAverageRuntimeByProblem(Map<String, List<RunResponse>> runsByProblem) {
        Map<String, Double> result = new LinkedHashMap<>();

        for (Map.Entry<String, List<RunResponse>> entry : runsByProblem.entrySet()) {
            String problemId = entry.getKey();
            List<RunResponse> runs = entry.getValue();

            double avg = runs.stream()
                    .mapToDouble(RunResponse::runtimeMs)
                    .average()
                    .orElse(0.0);

            result.put(problemId, avg);
        }

        return result;
    }


    public RuntimeStudyPointResponse toRuntimeStudyPoint(int problemSize, BatchRunResponse batch) {
        List<Double> values = new ArrayList<>();
        for (RunGroupResponse group : batch.batches()) {
            for (RunResponse run : group.runs()) {
                values.add((double) run.finalEvaluations());
            }
        }

        values.sort(Double::compareTo);

        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        List<Double> boxPlot = values.isEmpty()
                ? List.of()
                : List.of(
                values.getFirst(),
                percentile(values, 25),
                percentile(values, 50),
                percentile(values, 75),
                values.getLast()
        );
        return new RuntimeStudyPointResponse(problemSize, mean, boxPlot);
    }

    public SeriesWindowStatsResponse computeSeriesWindowStats(SeriesWindowStatsRequest request) {
        if (request == null) {
            throw new BadRequestException("Request body is required.");
        }
        if (request.points() == null || request.points().isEmpty()) {
            throw new BadRequestException("points must not be empty.");
        }
        if (!Double.isFinite(request.xMin()) || !Double.isFinite(request.xMax())) {
            throw new BadRequestException("xMin and xMax must be finite numbers.");
        }
        if (request.xMin() > request.xMax()) {
            throw new BadRequestException("xMin must be less than or equal to xMax.");
        }

        List<SeriesPoint> filtered = request.points().stream()
                .filter(p -> p != null && Double.isFinite(p.x()) && Double.isFinite(p.y()))
                .filter(p -> p.x() >= request.xMin() && p.x() <= request.xMax())
                .sorted(Comparator.comparingDouble(SeriesPoint::x))
                .toList();

        if (filtered.isEmpty()) {
            throw new BadRequestException("No points fall inside the requested x-range.");
        }

        List<Double> ys = filtered.stream().map(SeriesPoint::y).sorted().toList();
        List<Double> xs = filtered.stream().map(SeriesPoint::x).toList();

        int count = ys.size();
        double mean = ys.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = ys.stream().mapToDouble(y -> (y - mean) * (y - mean)).average().orElse(0.0);
        double stdDev = Math.sqrt(variance);
        double min = ys.getFirst();
        double max = ys.getLast();
        double median = percentile(ys, 50);
        double q1 = percentile(ys, 25);
        double q3 = percentile(ys, 75);
        double iqr = q3 - q1;

        double xMean = xs.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double covariance = 0.0;
        double xVariance = 0.0;
        for (SeriesPoint point : filtered) {
            double x = point.x();
            double y = point.y();
            covariance += (x - xMean) * (y - mean);
            xVariance += (x - xMean) * (x - xMean);
        }
        covariance /= count;
        xVariance /= count;
        double slope = xVariance == 0.0 ? 0.0 : covariance / xVariance;
        String trend = slope > TREND_THRESHOLD ? "up" : slope < -TREND_THRESHOLD ? "down" : "flat";

        return new SeriesWindowStatsResponse(
                request.seriesName(),
                request.xAxisLabel(),
                request.yAxisLabel(),
                request.xMin(),
                request.xMax(),
                count,
                min,
                max,
                mean,
                stdDev,
                median,
                q1,
                q3,
                iqr,
                slope,
                trend
        );
    }



    /**
     * Helper Methods
     */

    private RunResponse findReferenceRun(List<RunResponse> runs) {
        return runs.stream()
                .max(Comparator.comparingInt(a -> a.evaluations() != null ? a.evaluations().size() : 0))
                .orElse(runs.getFirst());
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
}