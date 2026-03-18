package dk.dtu.scout.backend.service;

import dk.dtu.scout.ConfigurationContext;
import dk.dtu.scout.acceptance.AcceptanceRule;
import dk.dtu.scout.backend.dto.RunRequest;
import dk.dtu.scout.backend.dto.run.BatchRunResponse;
import dk.dtu.scout.backend.dto.run.BatchSummaryResponse;
import dk.dtu.scout.backend.dto.run.RunGroupResponse;
import dk.dtu.scout.backend.dto.run.RunResponse;
import dk.dtu.scout.backend.exception.BadRequestException;
import dk.dtu.scout.generator.Generator;
import dk.dtu.scout.logging.RunLog;
import dk.dtu.scout.observer.*;
import dk.dtu.scout.population.PopulationModel;
import dk.dtu.scout.problems.Problem;
import dk.dtu.scout.searchSpace.SearchSpace;
import dk.dtu.scout.stopcondition.BroadcastStopCondition;
import dk.dtu.scout.stopcondition.MaxIterations;
import dk.dtu.scout.stopcondition.StopCondition;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Service
public class ExperimentService {

    private final StatisticsService statisticsService;
    private final ComponentRegistry<Generator> mutationRegistry;
    private final ComponentRegistry<AcceptanceRule> acceptanceRegistry;
    private final ComponentRegistry<PopulationModel> populationModelRegistry;
    private final ComponentRegistry<Problem> problemRegistry;
    private final ComponentRegistry<SearchSpace> searchSpaceRegistry;
    private final ComponentRegistry<StopCondition> stopConditionRegistry;
    private final ComponentRegistry<Observer> observerRegistry;

    public ExperimentService(
            StatisticsService statisticsService,
            ComponentRegistry<Generator> mutationRegistry,
            ComponentRegistry<AcceptanceRule> acceptanceRegistry,
            ComponentRegistry<PopulationModel> populationModelRegistry,
            ComponentRegistry<Problem> problemRegistry,
            ComponentRegistry<SearchSpace> searchSpaceRegistry,
            ComponentRegistry<StopCondition> stopConditionRegistry,
            ComponentRegistry<Observer> observerRegistry
    ) {
        this.statisticsService = statisticsService;
        this.mutationRegistry = mutationRegistry;
        this.acceptanceRegistry = acceptanceRegistry;
        this.populationModelRegistry = populationModelRegistry;
        this.problemRegistry = problemRegistry;
        this.searchSpaceRegistry = searchSpaceRegistry;
        this.stopConditionRegistry = stopConditionRegistry;
        this.observerRegistry = observerRegistry;
    }

    /**
     * Main method to run a batch of experiments based on the provided request.
     * It handles multiple runs, parallel execution, and aggregation of results.
     * @param request
     * @return
     */
    public BatchRunResponse run(RunRequest request) {
        long seed = request.seed();
        int runTimes = request.runTimes();
        if (request.searchSpaceId() == null || request.searchSpaceId().isEmpty()) {
            throw new BadRequestException("Search space must be specified");
        }
        String searchSpaceId = request.searchSpaceId().getFirst();

        return switch (searchSpaceId) {
            case "bitstring", "permutation" -> runBatch(
                    request,
                    seed,
                    runTimes,
                    () -> createSearchSpace(request.searchSpaceId(), request.searchSpaceParams())
            );
            default -> throw new BadRequestException("Unsupported search space: " + searchSpaceId);
        };
    }

    /**
     * Helper method to run a batch of experiments with parallel execution and result aggregation.
     * It creates multiple runs with different seeds, executes them in parallel,
     * and collects the results into a BatchRunResponse.
     * @param request
     * @param baseSeed
     * @param runtimes
     * @param searchSpaceFactory
     * @return
     * @param <S>
     */
    private <S> BatchRunResponse runBatch(
            RunRequest request,
            long baseSeed,
            int runtimes,
            Supplier<SearchSpace<S>> searchSpaceFactory
    ) {
        int threads = Math.max(1, Runtime.getRuntime().availableProcessors());
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        try {
            List<CompletableFuture<RunGroupResponse>> futures = new ArrayList<>();
            for (int i = 0; i < runtimes; i++) {
                final int runIndex = i;
                final long runSeed = baseSeed + i;

                CompletableFuture<RunGroupResponse> future = CompletableFuture.supplyAsync(() -> {
                    Random rng = new Random(runSeed);
                    SearchSpace<S> ss = searchSpaceFactory.get();

                    Generator<S> generator = createGenerator(request.generatorId(), request.generatorParams(), ss.dimension(), ss.id());
                    AcceptanceRule acceptance = createAcceptanceRule(request.acceptanceRuleId(), request.acceptanceRuleParams());
                    PopulationModel<S> popModel = createPopulationModel(request.populationModelId(), request.populationModelParams());

                    List<RunResponse> perProblemRuns = runTypedOnce(request, rng, ss, generator, acceptance, popModel);
                    return new RunGroupResponse(runIndex, runSeed, perProblemRuns);
                }, executor);

                futures.add(future);
            }

            List<RunGroupResponse> batches = futures.stream().map(CompletableFuture::join)
                    .sorted(Comparator.comparingInt(RunGroupResponse::runIndex)).toList();

            BatchSummaryResponse summary = statisticsService.calculateSummary(batches);
            return new BatchRunResponse(batches, summary);
        } finally {
            executor.shutdown();
        }
    }

    /**
     * Run a single execution of the algorithm on the specified problems,
     * collecting logs and results.
     * @param request
     * @param rng
     * @param ss
     * @param generator
     * @param acceptance
     * @param popModel
     * @return
     * @param <S>
     */
    private <S> List<RunResponse> runTypedOnce(
            RunRequest request,
            Random rng,
            SearchSpace<S> ss,
            Generator<S> generator,
            AcceptanceRule acceptance,
            PopulationModel<S> popModel
    ) {
        if (request.problemId() == null || request.problemId().isEmpty()) {
            throw new BadRequestException("Problem must be specified");
        }
        List<String> problemIds = request.problemId();
        List<RunResponse> runs = new ArrayList<>();

        for (String pid : problemIds) {
            Problem<S> problem = createProblem(pid, ss.dimension(), request.problemParams());
            StopCondition<S> stop = createStopConditionChain(request.stopConditionId(), request.stopConditionParams(), problem);
            Observer<S> observer = createObserverChain(request.observerIds(), problem);

            long startTime = System.nanoTime();
            RunLog log = popModel.run(generator, acceptance, ss, problem, rng, stop, observer);
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

    // ==================== Component Creation Methods ====================

    private <T> T createAndConfigure(
            ComponentRegistry<?> registry,
            List<String> ids,
            String componentType,
            Map<String, Object> params,
            ConfigurationContext context
    ) {
        if (ids == null || ids.isEmpty()) {
            throw new BadRequestException(componentType + " must be specified");
        }

        String id = ids.getFirst();
        Object component = registry.create(id);

        try {
            if (context != null) {
                try {
                    component.getClass()
                        .getMethod("configure", Map.class, ConfigurationContext.class)
                        .invoke(component, params != null ? params : Map.of(), context);
                    return (T) component;
                } catch (NoSuchMethodException e) {
                }
            }

            component.getClass()
                .getMethod("configure", Map.class)
                .invoke(component, params != null ? params : Map.of());
        } catch (Exception e) {
            throw new RuntimeException("Failed to configure component: " + component.getClass().getSimpleName(), e);
        }
        return (T) component;
    }

    private <S> SearchSpace<S> createSearchSpace(List<String> ids, Map<String, Object> params) {
        return createAndConfigure(searchSpaceRegistry, ids, "Search space", params, null);
    }

    private <S> Problem<S> createProblem(String id, int n, Map<String, Object> problemParams) {
        Problem<S> problem = (Problem<S>) problemRegistry.create(id);

        Map<String, Object> params = new HashMap<>(Map.of("n", n));
        if (problemParams != null) {
            params.putAll(problemParams);
        }

        problem.configure(params);
        return problem;
    }

    private <S> Generator<S> createGenerator(List<String> ids, Map<String, Object> params, int n, String searchSpaceId) {
        ConfigurationContext context = new ConfigurationContext(n);
        return createAndConfigure(mutationRegistry, ids, "Generator", params, context);
    }

    private AcceptanceRule createAcceptanceRule(List<String>  ids, Map<String, Object> params) {
        return createAndConfigure(acceptanceRegistry, ids, "Acceptance rule", params, null);
    }

    private<S>  PopulationModel<S> createPopulationModel(List<String>  ids, Map<String, Object> params) {
        return createAndConfigure(populationModelRegistry, ids, "Population model", params, null);
    }

    private<S> StopCondition<S> createStopConditionChain(List<String>  ids, Map<String, Object> params, Problem<S> problem) {
        if (ids == null || ids.isEmpty()) return new MaxIterations<>();
        final Map<String, Object> p = (params == null) ? Map.of() : params;
        List<StopCondition<S>> stops = ids.stream().map(id -> createSingleStopCondition(id, p, problem)).toList();
        return new BroadcastStopCondition<>(stops);
    }

    private<S> StopCondition<S>createSingleStopCondition(String id, Map<String, Object> params, Problem<S> problem) {
        ConfigurationContext context = new ConfigurationContext(0, problem);
        return createAndConfigure(stopConditionRegistry, List.of(id), "Stop condition", params, context);
    }

    private<S> Observer<S> createObserverChain(List<String> ids, Problem<S> problem) {
        if (ids == null || ids.isEmpty()) return new FitnessObserver<>();
        List<Observer<S>> obs = ids.stream().map(id -> this.<S>createSingleObserver(id, problem)).toList();
        return new BroadcastObserver<>(obs);
    }

    private <S>Observer<S> createSingleObserver(String id, Problem<S> problem) {
        ConfigurationContext context = new ConfigurationContext(0, problem);
        return createAndConfigure(observerRegistry, List.of(id), "Observer", Map.of(), context);
    }
}