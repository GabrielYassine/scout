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
import dk.dtu.scout.population.DefaultPopulationModel;
import dk.dtu.scout.population.PopulationModel;
import dk.dtu.scout.problems.LeadingOnesProblem;
import dk.dtu.scout.problems.OneMaxProblem;
import dk.dtu.scout.problems.Problem;
import dk.dtu.scout.mutation.BitMutation;
import dk.dtu.scout.searchSpace.BitString;
import dk.dtu.scout.searchSpace.SearchSpace;
import dk.dtu.scout.stopcondition.MaxIterations;
import dk.dtu.scout.stopcondition.OptimumReached;
import dk.dtu.scout.stopcondition.StopCondition;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;


@Service
public class ExperimentService {

    public RunResponse run(RunRequest request) {

        // Extract algorithm parameters with defaults (missing a few params)
        long seed = request.seed();


        // Initialize random number generator
        Random rng = new Random(seed);

        SearchSpace<boolean[]> ss = createSearchSpace(request.searchSpaceId(), request.searchSpaceParams());
        StopCondition<boolean[]> stop = (StopCondition<boolean[]>) createStopCondition(request.stopConditionId(), request.stopConditionParams());
        Problem<?> problem = createProblem(request.problemId(),  ss.dimension());
        Mutation<boolean[]> mutation = createMutation(request.mutationId(), request.mutationParams(), ss.dimension());
        AcceptanceRule acceptance = createAcceptanceRule(request.acceptanceRuleId(), request.acceptanceRuleParams());
        Observer<boolean[]> observer = createObserver(request.observerIds());

        // factory: Should create a new instance of the algorithm for each run in multistart
        Supplier<Algorithm<boolean[]>> algFactory = () -> (Algorithm<boolean[]>) createAlgorithm(request.algorithmId(), mutation, acceptance);

        PopulationModel<boolean[]> popModel = (PopulationModel<boolean[]>) createPopulationModel(request.populationModelId(), request.populationModelParams());

        RunLog log = popModel.run(algFactory,ss, (Problem<boolean[]>) problem, rng, stop, observer);
        return new RunResponse(
            request.problemId(),
            request.algorithmId(),
            log.getIterations(),
            log.getSeries()
        );
    }

    private SearchSpace<boolean[]> createSearchSpace(String id, Map<String,Object> params) {
        if (id == null) id = "bitstring";
        if (params == null) params = Map.of();

        SearchSpace<boolean[]> ss = switch (id) {
            case "bitstring" -> new BitString();
            default -> throw new IllegalArgumentException("Unknown search space: " + id);
        };

        ss.configure(params);
        return ss;
    }

    private Problem<?> createProblem(String id,  int n) {
        Problem<?> problem = switch (id) {
            case "onemax" -> new OneMaxProblem();
            case "leadingones" -> new LeadingOnesProblem();
            default -> throw new IllegalArgumentException("Unknown problem: " + id);
        };

        // Configure with parameters
        Map<String, Object> problemParams = Map.of("n", n);
        problem.configure(problemParams);
        return problem;
    }

    private Algorithm<?> createAlgorithm(String id, Mutation<boolean[]> mutation, AcceptanceRule acceptance) {
        Algorithm<boolean[]> algorithm = switch (id) {
            case "1p1-ea" -> {
                OnePlusOneEA<boolean[]> alg = new OnePlusOneEA<>();
                alg.setMutation(mutation);
                alg.setAcceptance(acceptance);
                yield alg;
            }
            case "sa" -> {
                SimulatedAnnealing<boolean[]> alg = new SimulatedAnnealing<>();
                alg.setMutation(mutation);
                alg.setAcceptance(acceptance);
                yield alg;
            }
            default -> throw new IllegalArgumentException("Unknown algorithm: " + id);
        };
        return algorithm;
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

        AcceptanceRule acceptance = switch (id) {
            case "elitist" -> new ElitistAcceptance();
            case "simulated-annealing" -> new SimulatedAnnealingAcceptance();
            default -> throw new IllegalArgumentException("Unknown acceptance rule: " + id);
        };

        // Configure with parameters
        acceptance.configure(params);
        return acceptance;
    }

    private PopulationModel<boolean[]> createPopulationModel(String id, Map<String, Object> params) {
        if (id == null) id = "default";
        if (params == null) params = Map.of();
        return switch (id) {
            case "default" -> new DefaultPopulationModel<>();
            default -> throw new IllegalArgumentException("Unknown population model: " + id);
        };
    }
    private StopCondition<?> createStopCondition(String id, Map<String, Object> params) {
        if (id == null) id = "max-iterations";
        if (params == null) params = Map.of();

        StopCondition<?> stop = switch (id) {
            case "max-iterations" -> new MaxIterations<>();
            case "optimum-reached" -> new OptimumReached<>();
            default -> throw new IllegalArgumentException("Unknown stop condition: " + id);
        };
        stop.configure(params);
        return stop;
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
}
