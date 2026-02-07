package dk.dtu.scout.backend.service;

import dk.dtu.scout.acceptance.AcceptanceRule;
import dk.dtu.scout.acceptance.ElitistAcceptance;
import dk.dtu.scout.acceptance.SimulatedAnnealingAcceptance;
import dk.dtu.scout.algorithms.Algorithm;
import dk.dtu.scout.algorithms.OnePlusOneEA;
import dk.dtu.scout.algorithms.SimulatedAnnealing;
import dk.dtu.scout.backend.dto.RunRequest;
import dk.dtu.scout.backend.dto.RunResponse;
import dk.dtu.scout.backend.util.FormulaEvaluator;
import dk.dtu.scout.logging.RunLog;
import dk.dtu.scout.mutation.Mutation;
import dk.dtu.scout.observer.*;
import dk.dtu.scout.problems.LeadingOnesProblem;
import dk.dtu.scout.problems.OneMaxProblem;
import dk.dtu.scout.problems.Problem;
import dk.dtu.scout.mutation.BitMutation;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Random;



@Service
public class ExperimentService {

    public RunResponse run(RunRequest request) {

        // Extract algorithm parameters with defaults (missing a few params)
        int maxIterations = ((Number) request.algorithmParams().getOrDefault("maxIterations", 1000)).intValue();
        long seed = request.seed();

        int n = ((Number) request.problemParams().getOrDefault("n", 100)).intValue();


        // Initialize random number generator
        Random rng = new Random(seed);

        Problem<?> problem = createProblem(request.problemId(), request.problemParams(), n);
        Mutation<boolean[]> mutation = createMutation(request.mutationId(), request.mutationParams(), n);
        AcceptanceRule acceptance = createAcceptanceRule(request.acceptanceRuleId(), request.acceptanceRuleParams());
        Observer<boolean[]> observer = createObserver(request.observerIds());

        // Create the algorithm based on the request
        Algorithm<?> algorithm = createAlgorithm(request.algorithmId(), mutation, acceptance);

        // The following log is resulting from running the algorithm on the problem
        RunLog log = runAlgorithm(algorithm, problem, rng, maxIterations, observer);

        return new RunResponse(
            request.problemId(),
            request.algorithmId(),
            log.getIterations(),
            log.getSeries()
        );
    }

    private Problem<?> createProblem(String id, Map<String, Object> params, int n) {
        return switch (id) {
            case "onemax" -> {
                yield new OneMaxProblem(n);
            }
            case "leadingones" -> {
                yield new LeadingOnesProblem(n);
            }
            default -> throw new IllegalArgumentException("Unknown problem: " + id);
        };
    }

    private Algorithm<?> createAlgorithm(String id, Mutation<boolean[]> mutation, AcceptanceRule acceptance) {
        return switch (id) {
            case "1p1-ea" -> new OnePlusOneEA<>(mutation, acceptance);
            case "sa" -> new SimulatedAnnealing<>(mutation, acceptance);
            default -> throw new IllegalArgumentException("Unknown algorithm: " + id);
        };
    }

    private Mutation<boolean[]> createMutation(String id, Map<String, Object> params, int n) {
        if (id == null) id = "bit-flip";
        if (params == null) params = Map.of();

        return switch (id) {
            case "bit-flip" -> {
                String formula = String.valueOf(params.getOrDefault("flipProbability", "1/n"));
                double p = FormulaEvaluator.eval(formula, n);
                p = Math.max(0.0, Math.min(1.0, p));
                yield BitMutation.withProbability(p);
            }
            case "single-bit-flip" -> BitMutation.singleBit();
            default -> throw new IllegalArgumentException("Unknown mutation: " + id);
        };
    }
    private AcceptanceRule createAcceptanceRule(String id, Map<String, Object> params) {
        if (id == null) id = "elitist";
        if (params == null) params = Map.of();

        return switch (id) {
            case "elitist" -> new ElitistAcceptance();

            case "simulated-annealing" -> {
                double T0 = ((Number) params.getOrDefault("initialTemperature", 5.0)).doubleValue();
                double coolingRate = ((Number) params.getOrDefault("coolingRate", 0.995)).doubleValue();
                double TMin = ((Number) params.getOrDefault("minTemperature", 1e-6)).doubleValue();
                yield new SimulatedAnnealingAcceptance(T0, coolingRate, TMin);
            }

            default -> throw new IllegalArgumentException("Unknown acceptance rule: " + id);
        };
    }

    private Observer<boolean[]> createObserver(List<String> observerIds) {
        // Default observer if none specified
        String observerId = (observerIds == null || observerIds.isEmpty())
            ? "fitness"
            : observerIds.get(0);

        return switch (observerId) {
            case "fitness" -> new FitnessObserver<>();
            case "acceptance-rate" -> new AcceptanceRateObserver<>();
            case "improvements" -> new ImprovementObserver<>();
            default -> throw new IllegalArgumentException("Unknown observer: " + observerId);
        };
    }

    @SuppressWarnings("unchecked")
    private RunLog runAlgorithm(Algorithm<?> algorithm, Problem<?> problem, Random rng, int maxIterations, Observer<?> observer) {
        return ((Algorithm<Object>) algorithm).run((Problem<Object>) problem, rng, maxIterations, (Observer<Object>) observer);
    }
}
