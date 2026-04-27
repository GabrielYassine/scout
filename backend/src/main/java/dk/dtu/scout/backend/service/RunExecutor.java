package dk.dtu.scout.backend.service;

import dk.dtu.scout.SimulationRunner;
import dk.dtu.scout.acceptance.SelectionRule;
import dk.dtu.scout.backend.dto.RunRequest;
import dk.dtu.scout.backend.dto.run.RunGroupResponse;
import dk.dtu.scout.backend.dto.run.RunResponse;
import dk.dtu.scout.backend.instance.InstanceMapper;
import dk.dtu.scout.backend.util.ViewMapper;
import dk.dtu.scout.backend.websocket.RunProgressObserver;
import dk.dtu.scout.backend.websocket.WsSender;
import dk.dtu.scout.crossover.Crossover;
import dk.dtu.scout.generator.Generator;
import dk.dtu.scout.logging.RunLog;
import dk.dtu.scout.observer.Observer;
import dk.dtu.scout.parentSelectionRule.ParentSelectionRule;
import dk.dtu.scout.population.PopulationModel;
import dk.dtu.scout.problems.Problem;
import dk.dtu.scout.searchSpace.SearchSpace;
import dk.dtu.scout.stopcondition.StopCondition;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.function.Supplier;

/**
 * Executes runs asynchronously.
 * RunOrchestratorService handles validation and run lifecycle management,
 * while this class focuses on executing the requested runs, collecting logs,
 * and sending WebSocket progress updates.
 * @author s235257 & Ahmed
 */
@Service
public class RunExecutor {

    private final RunComponentFactory factory;
    private final WsSender wsSender;
    private final ThreadPoolTaskExecutor runExecutor;

    public RunExecutor(
        RunComponentFactory factory,
        WsSender wsSender,
        @Qualifier("runTaskExecutor") Executor runExecutor
    ) {
        this.factory = factory;
        this.wsSender = wsSender;
        this.runExecutor = (ThreadPoolTaskExecutor) runExecutor;
    }

    /**
     * Executes a batch of runs based on the provided RunRequest. This batch can include multiple runtimes and multiple problems.
     * Each will generate a separate RunResponse, and all responses will be collected into a RunGroupResponse list.
     * @param request The RunRequest containing all necessary information to execute the runs.
     * @param logEveryIterations Determines how often to log progress for each run.
     * @param wsUpdateEveryIterations Determines how often to send WebSocket updates for each run.
     * @return A list of RunGroupResponse objects.
     * @param <S> The solution type used in the search space and problems. This is a generic type that allows the method to work with any solution representation defined in the search space.
     */
    public <S> List<RunGroupResponse> runBatch(
        RunRequest request,
        int logEveryIterations,
        int wsUpdateEveryIterations
    ) {
        checkCancelled();

        long baseSeed = request.seed(); // Will be altered for each runtime to ensure different random sequences
        int runtimes = request.runTimes();

        Map<String, Object> searchSpaceParams = copyParams(request.searchSpaceParams());
        Supplier<SearchSpace<S>> searchSpaceFactory = () -> factory.createSearchSpace(request.searchSpaceId(), searchSpaceParams);

        List<Future<RunGroupResponse>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < runtimes; i++) {
                checkCancelled();

                int runIndex = i;
                long runSeed = baseSeed + i;

                // Run each runtime asynchronously and collect the futures
                futures.add(runExecutor.submit(() ->
                    runSingleIndex(
                        request,
                        runIndex,
                        runSeed,
                        searchSpaceFactory,
                        logEveryIterations,
                        wsUpdateEveryIterations
                    )
                ));
            }
            List<RunGroupResponse> results = collectFinishedRuns(futures);
            // We return it sorted since there is no guarantee that parallel runs will finish in the order they were started.
            return results.stream().sorted(Comparator.comparingInt(RunGroupResponse::runIndex)).toList();
        } catch (RuntimeException ex) {
            cancelAll(futures);
            throw ex;
        }
    }

    /**
     * Prepares the tsp and vrp problems by giving them their instance object.
     * This includes mapping raw instance data to the appropriate problem-specific instance objects.
     * @param request The original RunRequest containing the problem parameters.
     * @param problemId The ID of the problem for which to prepare parameters. This is used to determine how to map instance data if present.
     * @return A map of problem parameters with any necessary transformations applied. The original parameters from the request are copied and then modified as needed based on the problem ID.
     */
    private Map<String, Object> prepareProblemParams(RunRequest request, String problemId) {
        Map<String, Object> problemParams = copyParams(request.problemParams());

        if ("tsp".equals(problemId) && problemParams.containsKey("tspInstance")) {
            problemParams.compute("tspInstance", (key, rawInstance) -> InstanceMapper.toTspInstance(asInstanceMap(rawInstance, "tspInstance")));
        }

        if ("vrp".equals(problemId) && problemParams.containsKey("vrpInstance")) {
            problemParams.compute("vrpInstance", (key, rawInstance) -> InstanceMapper.toVrpInstance(asInstanceMap(rawInstance, "vrpInstance")));
        }

        return problemParams;
    }

    /**
     * This is the core method that executes a single runtime
     * The runtime uses one seed and one search space, while each selected problem
     * is run sequentially within that runtime.
     * @param request The original RunRequest containing all necessary information to execute the runtime..
     * @param runIndex The index of the current runtime being executed.
     * @param runSeed The random seed to use for this runtime, which ensures that each runtime has a different random sequence.
     * @param searchSpaceFactory A supplier that creates a new instance of the SearchSpace for this runtime.
     * @param logEveryIterations Determines how often to log progress for this runtime.
     * @param wsUpdateEveryIterations Determines how often to send WebSocket updates for this runtime.
     * @return A RunGroupResponse containing the results of all problem runs within this runtime.
     * @param <S> The solution type used in the search space and problems.
     */
    private <S> RunGroupResponse runSingleIndex(
        RunRequest request,
        int runIndex,
        long runSeed,
        Supplier<SearchSpace<S>> searchSpaceFactory,
        int logEveryIterations,
        int wsUpdateEveryIterations
    ) {
        checkCancelled();

        Random rng = new Random(runSeed); // Different seed for each runtime.

        SearchSpace<S> searchSpace = searchSpaceFactory.get();
        Supplier<Generator<S>> generatorFactory = () -> factory.createGenerator(request.generatorId(), request.generatorParams(), searchSpace.id());

        List<RunResponse> perProblemRuns = new ArrayList<>();

        // Each problem in this specific runtime will be run sequentially, but the entire runtime can be run in parallel with other runtimes
        for (String problemId : request.problemIds()) {
            checkCancelled();

            Map<String, Object> problemParams = prepareProblemParams(request, problemId);

            Problem<S> problem = factory.createProblem(problemId, searchSpace.dimension(), problemParams);

            // Before creating the rest of the components, we validate that the problem and search space are compatible
            factory.validateProblemSearchSpaceCompatibility(problem, problemId, searchSpace.id());

            SelectionRule<S> selection = factory.createSelectionRule(request.selectionRuleId(), request.selectionRuleParams());
            List<StopCondition<S>> stopConditions = factory.createStopConditionChain(request.stopConditionIds(), request.stopConditionParams());
            List<Observer<S>> observers = new ArrayList<>(factory.createObservers(request.observerIds(), request.observerParams()));
            PopulationModel<S> populationModel = factory.createPopulationModel(request.populationModelId(), request.populationModelParams());
            Crossover<S> crossover = factory.createOptionalCrossover(request.crossoverId(), request.crossoverParams());
            ParentSelectionRule<S> parentSelection = factory.createParentSelectionRule(request.parentSelectionRuleId(), request.parentSelectionRuleParams());

            // By providing a RunProgressObserver, it will automatically update the frontend through ws at the specified intervals and at the end.
            if (request.runId() != null && wsUpdateEveryIterations > 0) {
                observers.add(new RunProgressObserver<>(
                    wsSender,
                    request.runId(),
                    runIndex,
                    runSeed,
                    searchSpace.id(),
                    problemId,
                    wsUpdateEveryIterations
                ));
            }

            long startTime = System.nanoTime();

            // Execute the simulation run with the specified components and collect the log
            RunLog log = new SimulationRunner().run(
                populationModel,
                generatorFactory,
                crossover,
                parentSelection,
                selection,
                searchSpace,
                problem,
                rng,
                stopConditions,
                observers,
                logEveryIterations
            );

            checkCancelled();

            double runtimeMs = (System.nanoTime() - startTime) / 1_000_000.0;

            List<Integer> evaluations = log.getEvaluations();
            int finalEvaluations = evaluations.isEmpty() ? 0 : evaluations.getLast();

            perProblemRuns.add(ViewMapper.toRunResponse(
                searchSpace.id(),
                problemId,
                log.getIterations(),
                evaluations,
                log.getSeries(),
                runtimeMs,
                finalEvaluations
            ));
        }

        return ViewMapper.toRunGroupResponse(runIndex, runSeed, perProblemRuns);
    }

    /**
     * Helper method to collect results from a list of futures representing asynchronous runs.
     * @param futures A list of Future objects.
     * @return A list of RunGroupResponse objects collected from the completed futures.
     */
    private List<RunGroupResponse> collectFinishedRuns(List<Future<RunGroupResponse>> futures) {
        List<RunGroupResponse> batches = new ArrayList<>(futures.size());

        for (Future<RunGroupResponse> future : futures) {
            checkCancelled();

            try {
                batches.add(future.get());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new CancellationException("Run cancelled");
            } catch (ExecutionException ex) {
                Throwable cause = ex.getCause();

                if (cause instanceof CancellationException cancellation) {
                    throw cancellation;
                }

                if (cause instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }

                throw new RuntimeException(cause);
            }
        }

        return batches;
    }

    private Map<String, Object> copyParams(Map<String, Object> params) {
        return params != null ? new LinkedHashMap<>(params) : new LinkedHashMap<>();
    }

    private void checkCancelled() {
        if (Thread.currentThread().isInterrupted()) {
            throw new CancellationException("Run cancelled");
        }
    }

    private void cancelAll(List<Future<RunGroupResponse>> futures) {
        for (Future<RunGroupResponse> future : futures) {
            future.cancel(true);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asInstanceMap(Object value, String label) {
        if (!(value instanceof Map<?, ?> raw)) {
            throw new IllegalArgumentException(label + " must be a map");
        }

        return (Map<String, Object>) raw;
    }
}