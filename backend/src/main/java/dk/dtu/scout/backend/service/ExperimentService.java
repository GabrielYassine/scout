package dk.dtu.scout.backend.service;

import dk.dtu.scout.algorithms.Algorithm;
import dk.dtu.scout.algorithms.OnePlusOneEA;
import dk.dtu.scout.backend.dto.RunRequest;
import dk.dtu.scout.backend.dto.RunResponse;
import dk.dtu.scout.datatypes.RunLog;
import dk.dtu.scout.problems.LeadingOnesProblem;
import dk.dtu.scout.problems.OneMaxProblem;
import dk.dtu.scout.problems.Problem;
import org.springframework.stereotype.Service;
import java.util.Map;


@Service
public class ExperimentService {

    public RunResponse run(RunRequest request) {

        Problem<?> problem = createProblem(
                request.problemId(),
                request.problemParams()
        );

        RunLog log = new RunLog();

        Algorithm algorithm = createAlgorithm(
                request.algorithmId(),
                request.algorithmParams(),
                problem,
                log
        );

        algorithm.run();

        return new RunResponse(
                request.problemId(),
                request.algorithmId(),
                log.getIterationSnapshots()
        );
    }

    //look at this again.
    private Problem<?> createProblem(String id, Map<String, Object> params) {
        return switch (id) {
            case "onemax" -> {
                int n = ((Number) params.getOrDefault("n", 100)).intValue();
                long seed = ((Number) params.getOrDefault("seed", 42L)).longValue();
                yield new OneMaxProblem(n, seed);
            }
            case "leadingones" -> {
                int n = ((Number) params.getOrDefault("n", 100)).intValue();
                long seed = ((Number) params.getOrDefault("seed", 42L)).longValue();
                yield new LeadingOnesProblem(n, seed);
            }
            default -> throw new IllegalArgumentException("Unknown problem: " + id);
        };
    }
    @SuppressWarnings("unchecked")
    private Algorithm createAlgorithm(String id, Map<String, Object> params, Problem<?> problem, RunLog log) {
        return switch (id) {
            case "1p1-ea" -> {
                int maxIterations = ((Number) params.getOrDefault("maxIterations", 1000)).intValue();
                long seed = ((Number) params.getOrDefault("seed", 42L)).longValue();
                yield new OnePlusOneEA<>((Problem<boolean[]>) problem, maxIterations, seed, log);
            }
            default -> throw new IllegalArgumentException("Unknown algorithm: " + id);
        };
    }

}
