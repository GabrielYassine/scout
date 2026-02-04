package dk.dtu.scout.backend.service;

import dk.dtu.scout.acceptance.AcceptanceRule;
import dk.dtu.scout.acceptance.ElitistAcceptance;
import dk.dtu.scout.acceptance.SimulatedAnnealingAcceptance;
import dk.dtu.scout.algorithms.Algorithm;
import dk.dtu.scout.algorithms.OnePlusOneEA;
import dk.dtu.scout.algorithms.SimulatedAnnealing;
import dk.dtu.scout.backend.dto.RunRequest;
import dk.dtu.scout.backend.dto.RunResponse;
import dk.dtu.scout.datatypes.RunLog;
import dk.dtu.scout.mutation.Mutation;
import dk.dtu.scout.problems.LeadingOnesProblem;
import dk.dtu.scout.problems.OneMaxProblem;
import dk.dtu.scout.problems.Problem;
import dk.dtu.scout.mutation.BitFlipMutation;
import dk.dtu.scout.mutation.SingleBitFlipMutation;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.Random;


@Service
public class ExperimentService {

    public RunResponse run(RunRequest request) {
        Problem<?> problem = createProblem(request.problemId(), request.problemParams());

        // Extract algorithm parameters with defaults (missing a few params)
        int maxIterations = ((Number) request.algorithmParams().getOrDefault("maxIterations", 1000)).intValue();
        long seed = ((Number) request.algorithmParams().getOrDefault("seed", 42L)).longValue();

        // Initialize random number generator
        Random rng = new Random(seed);

        // Create the algorithm based on the request
        Algorithm<?> algorithm = createAlgorithm(request.algorithmId(), request.algorithmParams());

        // The following log is resulting from running the algorithm on the problem
        RunLog<?> log = runAlgorithm(algorithm, problem, rng, maxIterations);

        return new RunResponse(request.problemId(), request.algorithmId(), log.getSnapshots());
    }

    private Problem<?> createProblem(String id, Map<String, Object> params) {
        return switch (id) {
            case "onemax" -> {
                int n = ((Number) params.getOrDefault("n", 100)).intValue();
                yield new OneMaxProblem(n);
            }
            case "leadingones" -> {
                int n = ((Number) params.getOrDefault("n", 100)).intValue();
                yield new LeadingOnesProblem(n);
            }
            default -> throw new IllegalArgumentException("Unknown problem: " + id);
        };
    }

    private Algorithm<?> createAlgorithm(String id,Map<String, Object> params) {
        return switch (id) {
            case "1p1-ea" -> {
                Mutation<boolean[]> mutation =  new BitFlipMutation();
                AcceptanceRule acceptance = new ElitistAcceptance();
                yield new OnePlusOneEA<>(mutation, acceptance);
            }
            case "sa" -> {
                double T0 = ((Number) params.getOrDefault("initialTemperature", 5.0)).doubleValue();
                double coolingRate = ((Number) params.getOrDefault("coolingRate", 0.995)).doubleValue();
                double TMin = ((Number) params.getOrDefault("minTemperature", 1e-6)).doubleValue();


                Mutation<boolean[]> mutation = new SingleBitFlipMutation();
                AcceptanceRule acceptance = new SimulatedAnnealingAcceptance(T0, coolingRate, TMin);
                yield new SimulatedAnnealing<>(mutation, acceptance);
            }
            default -> throw new IllegalArgumentException("Unknown algorithm: " + id);
        };
    }


    @SuppressWarnings("unchecked")
    private RunLog<?> runAlgorithm(Algorithm<?> algorithm, Problem<?> problem, Random rng, int maxIterations) {
        return ((Algorithm<Object>) algorithm).run((Problem<Object>) problem, rng, maxIterations);
    }
}
