package dk.dtu.scout.backend.service;

import dk.dtu.scout.backend.dto.run.BatchSummaryResponse;
import dk.dtu.scout.backend.dto.run.RunGroupResponse;
import dk.dtu.scout.backend.dto.run.RunResponse;
import dk.dtu.scout.backend.dto.run.RuntimeStats;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatisticsService {

    public BatchSummaryResponse calculateSummary(List<RunGroupResponse> batches) {
        Map<String, List<Double>> runtimesByProblem = new HashMap<>();
        Map<String, List<Integer>> finalEvaluationsByProblem = new HashMap<>();

        for (RunGroupResponse batch : batches) {
            for (RunResponse run : batch.runs()) {
                runtimesByProblem.computeIfAbsent(run.problemId(), k -> new ArrayList<>()).add(run.runtimeMs());
                finalEvaluationsByProblem.computeIfAbsent(run.problemId(), k -> new ArrayList<>()).add(run.finalEvaluations());
            }
        }

        Map<String, RuntimeStats> statsByProblem = new HashMap<>();
        for (Map.Entry<String, List<Double>> entry : runtimesByProblem.entrySet()) {
            String problemId = entry.getKey();
            List<Double> runtimes = entry.getValue();
            List<Integer> finalEvals = finalEvaluationsByProblem.get(problemId);
            statsByProblem.put(problemId, computeStats(runtimes, finalEvals));
        }

        return new BatchSummaryResponse(statsByProblem);
    }

    public RuntimeStats computeStats(List<Double> values, List<Integer> finalEvaluations) {
        int n = values.size();
        if (n == 0) {
            return new RuntimeStats(0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        }
        double finalEvaluationsMedian = calculateMedian(finalEvaluations);
        return new RuntimeStats(n, 0.0, 0.0, 0.0, 0.0, 0.0, finalEvaluationsMedian);
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
