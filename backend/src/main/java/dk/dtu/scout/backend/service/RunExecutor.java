package dk.dtu.scout.backend.service;

import dk.dtu.scout.SimulationRunner;
import dk.dtu.scout.acceptance.SelectionRule;
import dk.dtu.scout.backend.dto.RunRequest;
import dk.dtu.scout.backend.dto.run.RunGroupResponse;
import dk.dtu.scout.backend.dto.run.RunResponse;
import dk.dtu.scout.backend.instance.InstanceMapper;
import dk.dtu.scout.backend.util.ViewMapper;
import dk.dtu.scout.backend.websocket.MergeOp;
import dk.dtu.scout.backend.websocket.RunProgressObserver;
import dk.dtu.scout.backend.websocket.RunWsPayload;
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
     * Executes a batch of runs and returns the raw run groups.
     * Statistics are calculated by the caller, depending on whether the caller needs
     * a normal run summary or runtime-study statistics.
     * @param request the run request containing all parameters for the runs to execute
     * @param logEveryIterations how often to log progress in the RunLog for each run
     * @param wsUpdateEveryIterations how often to send WebSocket progress updates for each run
     * @return the completed run groups
     * @param <S> the solution representation type used by the selected search space
     */
    public <S> List<RunGroupResponse> runBatch(
            RunRequest request,
            int logEveryIterations,
            int wsUpdateEveryIterations
    ) {
        checkCancelled();

        long baseSeed = request.seed();
        int runtimes = request.runTimes();

        Map<String, Object> searchSpaceParams = prepareSearchSpaceParams(request);
        Supplier<SearchSpace<S>> searchSpaceFactory =
                () -> factory.createSearchSpace(request.searchSpaceId(), searchSpaceParams);

        List<Future<RunGroupResponse>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < runtimes; i++) {
                checkCancelled();

                int runIndex = i;
                long runSeed = baseSeed + i;

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

            return collectFinishedRuns(futures).stream()
                    .sorted(Comparator.comparingInt(RunGroupResponse::runIndex))
                    .toList();
        } catch (RuntimeException ex) {
            cancelAll(futures);
            throw ex;
        }
    }

    private Map<String, Object> prepareSearchSpaceParams(RunRequest request) {
        Map<String, Object> searchSpaceParams = request.searchSpaceParams() != null
                ? new LinkedHashMap<>(request.searchSpaceParams())
                : new LinkedHashMap<>();

        if ("route-list".equals(request.searchSpaceId()) && searchSpaceParams.containsKey("vrpInstance")) {
            searchSpaceParams.compute(
                    "vrpInstance",
                    (key, rawInstance) -> InstanceMapper.toVrpInstance(asInstanceMap(rawInstance, "vrpInstance"))
            );
        }

        return searchSpaceParams;
    }

    private Map<String, Object> prepareProblemParams(RunRequest request, String problemId) {
        Map<String, Object> problemParams = request.problemParams() != null
                ? new LinkedHashMap<>(request.problemParams())
                : new LinkedHashMap<>();

        if ("tsp".equals(problemId) && problemParams.containsKey("tspInstance")) {
            problemParams.compute(
                    "tspInstance",
                    (key, rawInstance) -> InstanceMapper.toTspInstance(asInstanceMap(rawInstance, "tspInstance"))
            );
        }

        if ("vrp".equals(problemId) && problemParams.containsKey("vrpInstance")) {
            problemParams.compute(
                    "vrpInstance",
                    (key, rawInstance) -> InstanceMapper.toVrpInstance(asInstanceMap(rawInstance, "vrpInstance"))
            );
        }

        return problemParams;
    }

    private <S> RunGroupResponse runSingleIndex(
            RunRequest request,
            int runIndex,
            long runSeed,
            Supplier<SearchSpace<S>> searchSpaceFactory,
            int logEveryIterations,
            int wsUpdateEveryIterations
    ) {
        checkCancelled();

        Random rng = new Random(runSeed);
        SearchSpace<S> searchSpace = searchSpaceFactory.get();

        Supplier<Generator<S>> generatorFactory =
                () -> factory.createGenerator(request.generatorId(), request.generatorParams(), searchSpace.id());

        List<RunResponse> perProblemRuns = new ArrayList<>();

        for (String problemId : request.problemIds()) {
            checkCancelled();

            Map<String, Object> problemParams = prepareProblemParams(request, problemId);

            Problem<S> problem = factory.createProblem(problemId, searchSpace.dimension(), problemParams);
            factory.validateProblemSearchSpaceCompatibility(problem, problemId, searchSpace.id());

            SelectionRule<S> selection =
                    factory.createSelectionRule(request.selectionRuleId(), request.selectionRuleParams());
            List<StopCondition<S>> stopConditions =
                    factory.createStopConditionChain(request.stopConditionIds(), request.stopConditionParams());
            List<Observer<S>> observers =
                    new ArrayList<>(factory.createObservers(request.observerIds(), request.observerParams()));
            PopulationModel<S> populationModel =
                    factory.createPopulationModel(request.populationModelId(), request.populationModelParams());
            Crossover<S> crossover =
                    factory.createOptionalCrossover(request.crossoverId(), request.crossoverParams());
            ParentSelectionRule<S> parentSelection =
                    factory.createParentSelectionRule(request.parentSelectionRuleId(), request.parentSelectionRuleParams());

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

            sendFinishedProgressIfNeeded(
                    request,
                    runIndex,
                    runSeed,
                    searchSpace,
                    problemId,
                    log,
                    wsUpdateEveryIterations,
                    runtimeMs
            );

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

    private <S> void sendFinishedProgressIfNeeded(
            RunRequest request,
            int runIndex,
            long runSeed,
            SearchSpace<S> searchSpace,
            String problemId,
            RunLog log,
            int wsUpdateEveryIterations,
            double runtimeMs
    ) {
        if (request.runId() == null || wsUpdateEveryIterations <= 0) {
            return;
        }

        int lastIteration = log.getIterations().isEmpty() ? 0 : log.getIterations().getLast();
        int lastEvaluation = log.getEvaluations().isEmpty() ? 0 : log.getEvaluations().getLast();

        wsSender.sendToRun(
                request.runId(),
                RunWsPayload.progress(
                        request.runId(),
                        runIndex,
                        runSeed,
                        searchSpace.id(),
                        problemId,
                        RunProgressObserver.nextSequenceIdFor(request.runId(), problemId, runSeed),
                        MergeOp.APPEND,
                        MergeOp.APPEND,
                        Map.of(),
                        lastIteration,
                        lastEvaluation,
                        null,
                        null,
                        Map.of(),
                        "FINISHED",
                        runtimeMs
                )
        );
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