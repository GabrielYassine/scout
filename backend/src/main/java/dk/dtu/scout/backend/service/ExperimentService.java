package dk.dtu.scout.backend.service;

import dk.dtu.scout.acceptance.AcceptanceRule;
import dk.dtu.scout.acceptance.ElitistAcceptance;
import dk.dtu.scout.acceptance.SimulatedAnnealingAcceptance;
import dk.dtu.scout.backend.dto.RunRequest;
import dk.dtu.scout.backend.dto.run.BatchRunResponse;
import dk.dtu.scout.backend.dto.run.BatchSummaryResponse;
import dk.dtu.scout.backend.dto.run.RunGroupResponse;
import dk.dtu.scout.backend.dto.run.RunResponse;
import dk.dtu.scout.backend.dto.run.RuntimeStats;
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
import dk.dtu.scout.problems.TSPInstance;
import dk.dtu.scout.problems.TSPProblem;
import dk.dtu.scout.mutation.BitFlipMutation;
import dk.dtu.scout.mutation.SwapMutation;
import dk.dtu.scout.mutation.TwoOptMutation;
import dk.dtu.scout.searchSpace.BitString;
import dk.dtu.scout.searchSpace.Permutation;
import dk.dtu.scout.searchSpace.SearchSpace;
import dk.dtu.scout.stopcondition.BroadcastStopCondition;
import dk.dtu.scout.stopcondition.MaxIterations;
import dk.dtu.scout.stopcondition.OptimumReached;
import dk.dtu.scout.stopcondition.StopCondition;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;


@Service
public class ExperimentService {

    private final TSPInstanceService tspInstanceService;

    public ExperimentService(TSPInstanceService tspInstanceService) {
        this.tspInstanceService = tspInstanceService;
    }

    /**
     * Will work as outer dispatcher.
     * @param request
     * @return
     */
    public BatchRunResponse run(RunRequest request) {
        long seed = request.seed();
        int runTimes = request.runTimes();

        AcceptanceRule acceptance = createAcceptanceRule(request.acceptanceRuleId(), request.acceptanceRuleParams());
        String searchSpaceId = firstOrDefault(request.searchSpaceId(), "bitstring");

        return switch (searchSpaceId) {
            case "bitstring" ->  {
                SearchSpace<boolean[]> ss = createSearchSpaceBoolean(request.searchSpaceId(), request.searchSpaceParams());
                Mutation<boolean[]> mutation = createMutationBoolean(request.mutationId(), request.mutationParams(), ss.dimension());
                PopulationModel<boolean[]> popModel = createPopulationModel(request.populationModelId(), request.populationModelParams());

                yield runTypedBatch(request, seed, runTimes, ss, mutation, acceptance, popModel,"onemax");
            }
            case "permutation" -> {
                SearchSpace<int[]> ss = createSearchSpaceInt(request.searchSpaceId(), request.searchSpaceParams());
                Mutation<int[]> mutation = createMutationInt(request.mutationId(), request.mutationParams(), ss.dimension());
                PopulationModel<int[]> popModel = createPopulationModel(request.populationModelId(), request.populationModelParams());

                yield runTypedBatch(request, seed, runTimes, ss, mutation, acceptance, popModel,"tsp");
            }
            default -> throw new IllegalArgumentException("Unsupported search space: " + searchSpaceId);

        };
    }

    private <S> BatchRunResponse runTypedBatch(
            RunRequest request,
            long baseSeed,
            int runtimes,
            SearchSpace<S> ss,
            Mutation<S> mutation,
            AcceptanceRule acceptance,
            PopulationModel<S> popModel,
            String defaultProblemId
    ) {
        List<RunGroupResponse> batches = new ArrayList<>();

        for (int i = 0; i < runtimes; i++) {
            long runSeed = baseSeed + i; // Simple way to get different seeds for each run, not ideal but works for now
            Random rng = new Random(runSeed);

            List<RunResponse> perProblemRuns = runTypedOnce(request, rng, ss, mutation, acceptance, popModel, defaultProblemId);
            batches.add(new RunGroupResponse(i, runSeed, perProblemRuns));
        }

        BatchSummaryResponse summary = calculateRuntimeStats(batches);
        return new BatchRunResponse(batches, summary);
    }

    private <S> List<RunResponse> runTypedOnce(
            RunRequest request,
            Random rng,
            SearchSpace<S> ss,
            Mutation<S> mutation,
            AcceptanceRule acceptance,
            PopulationModel<S> popModel,
            String defaultProblemId
    ) {
        List<String> problemIds = (request.problemId() == null || request.problemId().isEmpty()) ? List.of(defaultProblemId) : request.problemId();
        List<RunResponse> runs = new ArrayList<>();

        for (String pid : problemIds) {
            Problem<S> problem = createProblem(pid, ss.dimension(), ss.id(), request.problemParams());
            StopCondition<S> stop = createStopConditionChain(request.stopConditionId(), request.stopConditionParams(), problem);
            Observer<S> observer = createObserverChain(request.observerIds(), problem);

            long startTime = System.nanoTime();
            RunLog log = popModel.run(mutation, acceptance, ss, problem, rng, stop, observer);
            long endTime = System.nanoTime();
            double runtimeMs = (endTime - startTime) / 1_000_000.0;

            List<Integer> evaluations = log.getEvaluations();
            int finalEvaluations = evaluations.isEmpty() ? 0 : evaluations.getLast();

            runs.add(new RunResponse(
                    pid,
                    log.getIterations(),
                    evaluations,
                    log.getSeries(),
                    runtimeMs,
                    finalEvaluations
            ));
        }
        return runs;
    }

    private BatchSummaryResponse calculateRuntimeStats(List<RunGroupResponse> batches) {
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

    private RuntimeStats computeStats(List<Double> values, List<Integer> finalEvaluations) {
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
    private<S> Problem<S> createProblem(String  id,  int n, String searchSpaceId, Map<String, Object> problemParams) {
        return (Problem<S>) switch (searchSpaceId) {
            case "bitstring" -> createProblemBoolean(id, n, problemParams);
            case "permutation" -> createProblemInt(id, n, problemParams);
            default -> throw new IllegalArgumentException("Unsupported search space for problems: " + searchSpaceId);
        };
    }
    private Problem<boolean[]> createProblemBoolean(String id, int n, Map<String, Object> problemParams) {
        Problem<boolean[]> problem = switch (id) {
            case "onemax" -> new OneMaxProblem();
            case "leadingones" -> new LeadingOnesProblem();
            default -> throw new IllegalArgumentException("Unknown boolean problem: " + id);
        };

        Map<String, Object> params = new HashMap<>(Map.of("n", n));
        if (problemParams != null) {
            params.putAll(problemParams);
        }
        problem.configure(params);
        return problem;
    }

    private Problem<int[]> createProblemInt(String id, int n, Map<String, Object> problemParams) {
        Problem<int[]> problem = switch (id) {
            case "tsp" -> new TSPProblem();
            default -> throw new IllegalArgumentException("Unknown int[] problem: " + id);
        };

        Map<String, Object> params = new HashMap<>(Map.of("n", n));
        if (problemParams != null) {
            // Handle TSP instance conversion
            if (problemParams.containsKey("tspInstance")) {
                Object tspInstanceObj = problemParams.get("tspInstance");
                if (tspInstanceObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> tspInstanceMap = (Map<String, Object>) tspInstanceObj;
                    TSPInstance instance = convertToTSPInstance(tspInstanceMap);
                    params.put("instance", instance);
                }
            }
            // Copy other params
            for (Map.Entry<String, Object> entry : problemParams.entrySet()) {
                if (!entry.getKey().equals("tspInstance")) {
                    params.put(entry.getKey(), entry.getValue());
                }
            }
        }
        problem.configure(params);
        return problem;
    }

    private TSPInstance convertToTSPInstance(Map<String, Object> tspInstanceMap) {
        String name = (String) tspInstanceMap.getOrDefault("name", "Custom Instance");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> citiesList = (List<Map<String, Object>>) tspInstanceMap.get("cities");

        if (citiesList == null || citiesList.isEmpty()) {
            throw new IllegalArgumentException("TSP instance must have cities");
        }

        int dimension = citiesList.size();
        double[][] coordinates = new double[dimension][2];

        for (int i = 0; i < dimension; i++) {
            Map<String, Object> city = citiesList.get(i);
            Object xObj = city.get("x");
            Object yObj = city.get("y");

            double x = (xObj instanceof Number) ? ((Number) xObj).doubleValue() : 0.0;
            double y = (yObj instanceof Number) ? ((Number) yObj).doubleValue() : 0.0;

            coordinates[i][0] = x;
            coordinates[i][1] = y;
        }

        return new TSPInstance(name, dimension, coordinates);
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
        String id = firstOrDefault(ids, "swap");
        if (params == null) params = Map.of();

        Mutation<int[]> mutation = switch (id) {
            case "swap" -> new SwapMutation();
            case "2opt" -> new TwoOptMutation();
            default -> throw new IllegalArgumentException("Unknown int[] mutation: " + id);
        };

        mutation.configure(params);
        return mutation;
    }

    private AcceptanceRule createAcceptanceRule(List<String>  ids, Map<String, Object> params) {
        String id =  firstOrDefault( ids, "elitist");
        if (params == null) params = Map.of();

        AcceptanceRule acceptance = switch (id) {
            case "elitist" -> new ElitistAcceptance();
            case "simulated-annealing" -> new SimulatedAnnealingAcceptance();
            default -> throw new IllegalArgumentException("Unknown acceptance rule: " + id);
        };

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

    private<S> Observer<S> createObserverChain(List<String> ids, Problem<S> problem) {
        if (ids == null || ids.isEmpty()) return new FitnessObserver<>();

        List<Observer<S>> obs = ids.stream().map(id -> this.<S>createSingleObserver(id, problem)).toList();
        return new BroadcastObserver<>(obs);
    }

    @SuppressWarnings("unchecked")
    private <S>Observer<S> createSingleObserver(String id, Problem<S> problem) {
        Observer<S> observer = switch (id) {
            case "fitness" -> new FitnessObserver<>();
            case "acceptance-rate" -> new AcceptanceRateObserver<>();
            case "improvements" -> new ImprovementObserver<>();
            case "hypercube" -> (Observer<S>) new HypercubeObserver();
            case "tsp-tour" -> {
                TSPTourObserver tspObs = new TSPTourObserver();
                // Configure with cities if problem is TSP
                if (problem instanceof TSPProblem) {
                    TSPProblem tspProblem = (TSPProblem) problem;
                    TSPInstance instance = tspProblem.getInstance();
                    if (instance != null) {
                        double[][] coords = instance.getCoordinates();
                        List<Map<String, Double>> cities = new ArrayList<>();
                        for (int i = 0; i < coords.length; i++) {
                            Map<String, Double> city = new HashMap<>();
                            city.put("x", coords[i][0]);
                            city.put("y", coords[i][1]);
                            cities.add(city);
                        }
                        tspObs.setCities(cities);
                    }
                }
                yield (Observer<S>) tspObs;
            }
            default -> throw new IllegalArgumentException("Unknown observer: " + id);
        };
        return observer;
    }

    /**
     * Helper method to get the first element of a list or return a default value if the list is null or empty.
     */
    private String firstOrDefault(List<String> ids, String def) {
        return (ids == null || ids.isEmpty()) ? def : ids.get(0);
    }
}
