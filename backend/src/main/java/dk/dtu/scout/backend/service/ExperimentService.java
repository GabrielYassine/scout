package dk.dtu.scout.backend.service;

import dk.dtu.scout.acceptance.AcceptanceRule;
import dk.dtu.scout.acceptance.ElitistAcceptance;
import dk.dtu.scout.acceptance.SimulatedAnnealingAcceptance;
import dk.dtu.scout.algorithms.Algorithm;
import dk.dtu.scout.algorithms.OnePlusOneEA;
import dk.dtu.scout.algorithms.SimulatedAnnealing;
import dk.dtu.scout.backend.dto.BatchRunResponse;
import dk.dtu.scout.backend.dto.RunRequest;
import dk.dtu.scout.backend.dto.RunResponse;
import dk.dtu.scout.backend.util.FormulaEvaluator;
import dk.dtu.scout.logging.RunLog;
import dk.dtu.scout.mutation.Mutation;
import dk.dtu.scout.mutation.SingleBitFlipMutation;
import dk.dtu.scout.observer.*;
import dk.dtu.scout.population.DefaultPopulationModel;
import dk.dtu.scout.population.IslandModel;
import dk.dtu.scout.population.PopulationModel;
import dk.dtu.scout.problems.LeadingOnesProblem;
import dk.dtu.scout.problems.OneMaxProblem;
import dk.dtu.scout.problems.Problem;
import dk.dtu.scout.mutation.BitFlipMutation;
import dk.dtu.scout.searchSpace.BitString;
import dk.dtu.scout.searchSpace.SearchSpace;
import dk.dtu.scout.stopcondition.BroadcastStopCondition;
import dk.dtu.scout.stopcondition.MaxIterations;
import dk.dtu.scout.stopcondition.OptimumReached;
import dk.dtu.scout.stopcondition.StopCondition;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;


@Service
public class ExperimentService {

    public BatchRunResponse run(RunRequest request) {

        // Initialize random number generator
        long seed = request.seed();
        Random rng = new Random(seed);

        // Create components based on request
        SearchSpace<boolean[]> ss = createSearchSpace(request.searchSpaceId(), request.searchSpaceParams());
        Mutation<boolean[]> mutation = createMutation(request.mutationId(), request.mutationParams(), ss.dimension());
        AcceptanceRule acceptance = createAcceptanceRule(request.acceptanceRuleId(), request.acceptanceRuleParams());
        PopulationModel<boolean[]> popModel = createPopulationModel(request.populationModelId(), request.populationModelParams());
        List<String> problemIds = (request.problemId() == null || request.problemId().isEmpty()) ? List.of("onemax") : request.problemId();
        List<RunResponse> runs =new ArrayList<>();

        for (String pid : problemIds) {
            Problem<?> problem = createProblem(pid, ss.dimension());
            Observer<boolean[]> observer = createObserverChain(request.observerIds());
            StopCondition<boolean[]> stop = (StopCondition<boolean[]>) createStopConditionChain(request.stopConditionId(), request.stopConditionParams(), problem);

            // Create a fresh aglorithm instance for each population model run
            Supplier<Algorithm<boolean[]>> algFactory = () -> (Algorithm<boolean[]>) createAlgorithm(request.algorithmId(), mutation, acceptance);
            RunLog log = popModel.run(algFactory, ss, (Problem<boolean[]>) problem, rng,stop, observer);

            runs.add(new RunResponse(
                    pid,
                    request.algorithmId().getFirst(),
                    log.getIterations(),
                    log.getSeries()
            ));
        }
        return new BatchRunResponse(runs);
    }

    private SearchSpace<boolean[]> createSearchSpace(List<String>  ids, Map<String,Object> params) {
        String id = (ids == null || ids.isEmpty())
                ? "bitstring"
                : ids.get(0);

        if (params == null) params = Map.of();

        SearchSpace<boolean[]> ss = switch (id) {
            case "bitstring" -> new BitString();
            default -> throw new IllegalArgumentException("Unknown search space: " + id);
        };

        ss.configure(params);
        return ss;
    }

    private Problem<?> createProblem(String  id,  int n) {
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

    private Algorithm<?> createAlgorithm(List<String>  ids, Mutation<boolean[]> mutation, AcceptanceRule acceptance) {
        String id = (ids == null || ids.isEmpty())
                ? "1p1-ea"
                : ids.get(0);
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

    private Mutation<boolean[]> createMutation(List<String>  ids, Map<String, Object> params, int n) {
        String id = (ids == null || ids.isEmpty())
                ? "bit-flip"
                : ids.get(0);
        if (params == null) params = Map.of();

        return switch (id) {
            case "bit-flip" -> {
                String formula = String.valueOf(params.getOrDefault("flipProbability", "1/n"));
                double p = FormulaEvaluator.eval(formula, n);
                p = Math.max(0.0, Math.min(1.0, p));
                BitFlipMutation m = new BitFlipMutation();
                m.configure(Map.of("flipProbability", p));
                yield m;
            }
            case "single-bit-flip" -> new SingleBitFlipMutation();
            default -> throw new IllegalArgumentException("Unknown mutation: " + id);
        };
    }

    private AcceptanceRule createAcceptanceRule(List<String>  ids, Map<String, Object> params) {
        String id = (ids == null || ids.isEmpty())
                ? "elitist"
                : ids.get(0);
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

    private PopulationModel<boolean[]> createPopulationModel(List<String>  ids, Map<String, Object> params) {
        String id = (ids == null || ids.isEmpty())
                ? "default"
                : ids.get(0);
        if (params == null) params = Map.of();
        return switch (id) {
            case "default" -> new DefaultPopulationModel<>();
            case "islands" -> new IslandModel<>();
            default -> throw new IllegalArgumentException("Unknown population model: " + id);
        };
    }
    private StopCondition<?> createStopConditionChain(List<String>  ids, Map<String, Object> params, Problem<?> problem) {
        if (ids == null || ids.isEmpty()) return new MaxIterations<>();
        final Map<String, Object> p = (params == null) ? Map.of() : params;

        List<StopCondition<boolean[]>> stops = ids.stream().map(id -> createSingleStopCondition(id, p,problem)).toList();

        return new BroadcastStopCondition<>(stops);
    }
    private StopCondition<boolean[]> createSingleStopCondition(String id, Map<String, Object> params, Problem<?> problem) {
        StopCondition<boolean[]> stop = switch (id) {
            case "max-iterations" -> new MaxIterations<>();
            case "optimum-reached" ->{
                OptimumReached<boolean[]> s = new OptimumReached<>();
                s.setProblem(problem);
                yield s;
            }
            default -> throw new IllegalArgumentException("Unknown stop condition: " + id);
        };
        stop.configure(params);
        return stop;
    }

    private Observer<boolean[]> createObserverChain(List<String> ids) {
        if (ids == null || ids.isEmpty()) return new FitnessObserver<>();

        List<Observer<boolean[]>> obs = ids.stream().map(this::createSingleObserver).toList();
        return new BroadcastObserver<>(obs);
    }

    private Observer<boolean[]> createSingleObserver(String id) {
        return switch (id) {
            case "fitness" -> new FitnessObserver<>();
            case "acceptance-rate" -> new AcceptanceRateObserver<>();
            case "improvements" -> new ImprovementObserver<>();
            default -> throw new IllegalArgumentException("Unknown observer: " + id);
        };
    }

}
