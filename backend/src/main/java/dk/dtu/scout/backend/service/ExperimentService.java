package dk.dtu.scout.backend.service;

import dk.dtu.scout.Parameter;
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
import dk.dtu.scout.searchSpace.Permutation;
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
        AcceptanceRule acceptance = createAcceptanceRule(request.acceptanceRuleId(), request.acceptanceRuleParams());
        String searchSpaceId=firstOrDefault(request.searchSpaceId(), "bitstring");
        return switch (searchSpaceId) {
            case "bitstring" ->  {
                SearchSpace<boolean[]> ss = createSearchSpaceBoolean(request.searchSpaceId(), request.searchSpaceParams());
                Mutation<boolean[]> mutation = createMutationBoolean(request.mutationId(), request.mutationParams(), ss.dimension());
                PopulationModel<boolean[]> popModel = createPopulationModel(request.populationModelId(), request.populationModelParams());
                yield runTyped(request, rng, ss, mutation, acceptance, popModel,"onemax");
            }
            case "permutation" -> {
                SearchSpace<int[]> ss = createSearchSpaceInt(request.searchSpaceId(), request.searchSpaceParams());
                Mutation<int[]> mutation = createMutationInt(request.mutationId(), request.mutationParams(), ss.dimension());
                PopulationModel<int[]> popModel = createPopulationModel(request.populationModelId(), request.populationModelParams());
                yield runTyped(request, rng, ss, mutation, acceptance, popModel,"tsp");
            }
            default -> throw new IllegalArgumentException("Unsupported search space: " + searchSpaceId);

        };
    }

    public<S>  BatchRunResponse runTyped(RunRequest request, Random rng, SearchSpace<S> ss, Mutation<S> mutation, AcceptanceRule acceptance, PopulationModel<S> popModel,  String defaultProblemId) {
        List<String> problemIds = (request.problemId() == null || request.problemId().isEmpty()) ? List.of(defaultProblemId) : request.problemId();
        List<RunResponse> runs =new ArrayList<>();
        for (String pid : problemIds) {
            Problem<S> problem = createProblem(pid, ss.dimension(), ss.id());
            StopCondition<S> stop = createStopConditionChain(request.stopConditionId(), request.stopConditionParams(), problem);
            Observer <S> observer = createObserverChain(request.observerIds());
            // Create a fresh aglorithm instance for each population model run
            Supplier<Algorithm<S>> algFactory = () -> createAlgorithm(request.algorithmId(), mutation, acceptance);
            RunLog log = popModel.run(algFactory, ss, problem, rng,stop, observer);

            runs.add(new RunResponse(
                    pid,
                    request.algorithmId().getFirst(),
                    log.getIterations(),
                    log.getSeries()
            ));
        }
        return new BatchRunResponse(runs);
    }

    private SearchSpace<boolean[]> createSearchSpaceBoolean(List<String>  ids, Map<String,Object> params) {
        String id =  firstOrDefault( ids, "bitstring");
        if (params == null) params = Map.of();
        SearchSpace<boolean[]> ss = switch (id) {
            case "bitstring" -> new BitString();
            default -> throw new IllegalArgumentException("Unknown search space: " + id);
        };
        ss.configure(params);
        return ss;
    }
    private SearchSpace<int[]> createSearchSpaceInt(List<String>  ids, Map<String,Object> params) {
        String id =  firstOrDefault( ids, "permutation");

        if (params == null) params = Map.of();

        SearchSpace<int[]> ss = switch (id) {
            case "permutation" -> new Permutation();
            default -> throw new IllegalArgumentException("Unknown search space: " + id);
        };
        ss.configure(params);
        return ss;
    }

    @SuppressWarnings("unchecked")
    private<S> Problem<S> createProblem(String  id,  int n, String searchSpaceId) {
        return (Problem<S>) switch (searchSpaceId) {
            case "bitstring" -> createProblemBoolean(id, n);
            case "permutation" -> createProblemInt(id, n);
            default -> throw new IllegalArgumentException("Unsupported search space for problems: " + searchSpaceId);
        };
    }
    private Problem<boolean[]> createProblemBoolean(String id, int n) {
        Problem<boolean[]> problem = switch (id) {
            case "onemax" -> new OneMaxProblem();
            case "leadingones" -> new LeadingOnesProblem();
            default -> throw new IllegalArgumentException("Unknown boolean problem: " + id);
        };

        Map<String, Object> problemParams = Map.of("n", n);
        problem.configure(problemParams);
        return problem;
    }

    private Problem<int[]> createProblemInt(String id, int n) {
        throw new IllegalArgumentException(
                "Problem<int[]> not wired yet for id='" + id + "'. Add your int[] problems here."
        );
    }

    private <S>Algorithm<S> createAlgorithm(List<String>  ids, Mutation<S> mutation, AcceptanceRule acceptance) {
        String id =  firstOrDefault( ids, "1p1-ea");

        return switch (id) {
            case "1p1-ea" -> {
                OnePlusOneEA<S> alg = new OnePlusOneEA<>();
                alg.setMutation(mutation);
                alg.setAcceptance(acceptance);
                yield alg;
            }
            case "sa" -> {
                SimulatedAnnealing<S> alg = new SimulatedAnnealing<>();
                alg.setMutation(mutation);
                alg.setAcceptance(acceptance);
                yield alg;
            }
            default -> throw new IllegalArgumentException("Unknown algorithm: " + id);
        };
    }

    private Mutation<boolean[]> createMutationBoolean(List<String>  ids, Map<String, Object> params, int n) {
        String id =  firstOrDefault( ids, "bit-flip");
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
    private Mutation<int[]> createMutationInt(List<String>  ids, Map<String, Object> params, int n) {
       //toDO
        throw new IllegalArgumentException("Unknown mutation: ");
    }

    private AcceptanceRule createAcceptanceRule(List<String>  ids, Map<String, Object> params) {
        String id =  firstOrDefault( ids, "elitist");
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

    private<S>  PopulationModel<S> createPopulationModel(List<String>  ids, Map<String, Object> params) {
        String id =  firstOrDefault( ids, "default");
        if (params == null) params = Map.of();
        PopulationModel<S> model =switch (id) {
            case "default" -> new DefaultPopulationModel<>();
            case "islands" -> new IslandModel<>();
            default -> throw new IllegalArgumentException("Unknown population model: " + id);
        };
        model.configure(params);
        return model;
    }
    private<S> StopCondition<S> createStopConditionChain(List<String>  ids, Map<String, Object> params, Problem<S> problem) {
        if (ids == null || ids.isEmpty()) return new MaxIterations<>();
        final Map<String, Object> p = (params == null) ? Map.of() : params;


        List<StopCondition<S>> stops = ids.stream().map(id -> createSingleStopCondition(id, p, problem)).toList();

        return new BroadcastStopCondition<>(stops);
    }
    private<S> StopCondition<S>createSingleStopCondition(String id, Map<String, Object> params, Problem<S> problem) {
        StopCondition<S> stop = switch (id) {
            case "max-iterations" -> new MaxIterations<>();
            case "optimum-reached" ->{
                OptimumReached<S> s = new OptimumReached<>();
                s.setProblem(problem);
                yield s;
            }
            default -> throw new IllegalArgumentException("Unknown stop condition: " + id);
        };
        stop.configure(params);
        return stop;
    }

    private<S> Observer<S> createObserverChain(List<String> ids) {
        if (ids == null || ids.isEmpty()) return new FitnessObserver<>();

        List<Observer<S>> obs = ids.stream().map(this::<S>createSingleObserver).toList();
        return new BroadcastObserver<>(obs);
    }

    private <S>Observer<S> createSingleObserver(String id) {
        return switch (id) {
            case "fitness" -> new FitnessObserver<>();
            case "acceptance-rate" -> new AcceptanceRateObserver<>();
            case "improvements" -> new ImprovementObserver<>();
            default -> throw new IllegalArgumentException("Unknown observer: " + id);
        };
    }

    /**
     * Helper method to get the first element of a list or return a default value if the list is null or empty.
     */
    private String firstOrDefault(List<String> ids, String def) {
        return (ids == null || ids.isEmpty()) ? def : ids.get(0);
    }
}
