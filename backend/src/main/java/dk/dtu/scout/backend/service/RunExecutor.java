package dk.dtu.scout.backend.service;

import dk.dtu.scout.SimulationRunner;
import dk.dtu.scout.acceptance.SelectionRule;
import dk.dtu.scout.backend.dto.RunRequest;
import dk.dtu.scout.backend.dto.run.BatchRunResponse;
import dk.dtu.scout.backend.dto.run.BatchSummaryResponse;
import dk.dtu.scout.backend.dto.run.RunGroupResponse;
import dk.dtu.scout.backend.dto.run.RunResponse;
import dk.dtu.scout.backend.util.ViewMapper;
import dk.dtu.scout.backend.util.InstanceMapper;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * Executes batches of runs using a shared executor and aggregates results.
 */
@Service
public class RunExecutor {

    private static final Logger logger = LoggerFactory.getLogger(RunExecutor.class);

    private final RunComponentFactory factory;
    private final StatisticsService statisticsService;
    private final WsSender wsSender;
    private final ThreadPoolTaskExecutor runExecutor;

    public RunExecutor(
            RunComponentFactory factory,
            StatisticsService statisticsService,
            WsSender wsSender,
            @Qualifier("runTaskExecutor") Executor runExecutor
    ) {
        this.factory = factory;
        this.statisticsService = statisticsService;
        this.wsSender = wsSender;
        this.runExecutor = (ThreadPoolTaskExecutor) runExecutor;
    }

    private void checkCancelled() {
        if (Thread.currentThread().isInterrupted()) {
            throw new CancellationException("Run cancelled");
        }
    }

    public BatchRunResponse executeBatch(RunRequest request, int logEveryIterations, int wsUpdateEveryIterations) {
        checkCancelled();
        return runBatch(request, logEveryIterations, wsUpdateEveryIterations);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private <S> BatchRunResponse runBatch(RunRequest request, int logEveryIterations, int wsUpdateEveryIterations) {
        checkCancelled();
        long baseSeed = request.seed();
        int runtimes = request.runTimes();
        String runId = request.runId();

        Map<String, Object> searchSpaceParams = request.searchSpaceParams() != null
                ? new LinkedHashMap<>(request.searchSpaceParams())
                : new LinkedHashMap<>();

        if ("route-list".equals(request.searchSpaceId()) && searchSpaceParams.containsKey("vrpInstance")) {
            searchSpaceParams.compute("vrpInstance", (k, rawInstance) -> InstanceMapper.toVrpInstance(rawInstance));
        }

        Supplier<SearchSpace<S>> searchSpaceFactory = () -> factory.createSearchSpace(request.searchSpaceId(), searchSpaceParams);

        long batchStartTime = System.nanoTime();

        List<Future<RunGroupResponse>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < runtimes; i++) {
                checkCancelled();
                final int runIndex = i;
                final long runSeed = baseSeed + i;
                futures.add(runExecutor.submit(() -> runSingleIndex(request, runIndex, runSeed, searchSpaceFactory, logEveryIterations, wsUpdateEveryIterations)));
            }

            logExecutorStats("run-batch-start", runtimes, runId);

            List<RunGroupResponse> batches = new ArrayList<>(runtimes);
            for (Future<RunGroupResponse> f : futures) {
                checkCancelled();
                try {
                    batches.add(f.get());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new CancellationException("Run cancelled");
                } catch (ExecutionException ee) {
                    Throwable cause = ee.getCause();
                    if (cause instanceof CancellationException) {
                        throw (CancellationException) cause;
                    }
                    if (cause instanceof RuntimeException re) {
                        throw re;
                    }
                    throw new RuntimeException(cause);
                }
            }

            batches = batches.stream()
                .sorted(Comparator.comparingInt(RunGroupResponse::runIndex))
                .toList();

            long batchEndTime = System.nanoTime();
            double batchExecutionTimeMs = (batchEndTime - batchStartTime) / 1_000_000.0;

            logger.info("run-batch-execution-time runId={} searchSpaceId={} runtimes={} batchExecutionTimeMs={}", runId, request.searchSpaceId(), runtimes, batchExecutionTimeMs);

            BatchSummaryResponse summary = statisticsService.calculateSummary(batches);
            BatchRunResponse response = ViewMapper.toBatchRunResponse(runId, batches, summary);
            logExecutorStats("run-batch-end", runtimes, runId);
            return response;
        } catch (CancellationException ex) {
            // Best-effort: cancel outstanding child tasks.
            for (Future<RunGroupResponse> f : futures) {
                f.cancel(true);
            }
            throw ex;
        } catch (RuntimeException ex) {
            for (Future<RunGroupResponse> f : futures) {
                f.cancel(true);
            }
            throw ex;
        }
    }



    private void logExecutorStats(String phase, int runs, String runId) {
        var pool = runExecutor.getThreadPoolExecutor();
        int active = pool.getActiveCount();
        int queued = pool.getQueue().size();
        int poolSize = pool.getPoolSize();
        logger.info("{} runId={} runs={} activeThreads={} poolSize={} queueDepth={}", phase, runId, runs, active, poolSize, queued);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
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
        SearchSpace<S> ss = searchSpaceFactory.get();

        Supplier<Generator<S>> generatorFactory = () -> factory.createGenerator(request.generatorId(), request.generatorParams(), ss.id());

        List<RunResponse> perProblemRuns = new ArrayList<>();
        for (String pid : request.problemIds()) {
            checkCancelled();
            Map<String, Object> problemParams = request.problemParams() != null
                    ? new LinkedHashMap<>(request.problemParams())
                    : new LinkedHashMap<>();

            if ("tsp".equals(pid) && problemParams.containsKey("tspInstance")) {
                problemParams.compute("tspInstance", (k, rawInstance) -> InstanceMapper.toTspInstance(rawInstance));
            }
            if ("vrp".equals(pid) && problemParams.containsKey("vrpInstance")) {
                problemParams.compute("vrpInstance", (k, rawInstance) -> InstanceMapper.toVrpInstance(rawInstance));
            }

            Problem<S> problem = factory.createProblem(pid, ss.dimension(), problemParams);
            factory.validateProblemSearchSpaceCompatibility(problem, pid, ss.id());

            SelectionRule selection = factory.createSelectionRule(request.selectionRuleId(), request.selectionRuleParams());
            List<StopCondition<S>> stopConditions = factory.createStopConditionChain(request.stopConditionIds(), request.stopConditionParams());
            List<Observer<S>> observers = new ArrayList<>(factory.createObservers(request.observerIds(), request.observerParams()));
            PopulationModel<S> popModel = factory.createPopulationModel(request.populationModelId(), request.populationModelParams());
            Crossover<S> crossover = factory.createOptionalCrossover(request.crossoverId(), request.crossoverParams());
            ParentSelectionRule<S> parentSelection = factory.createParentSelectionRule(request.parentSelectionRuleId(), request.parentSelectionRuleParams());
            if (request.runId() != null && wsUpdateEveryIterations > 0) {
                observers.add(new RunProgressObserver<>(wsSender, request.runId(), runIndex, runSeed,  ss.id(), pid, wsUpdateEveryIterations));
            }
            long startTime = System.nanoTime();

            SimulationRunner runner = new SimulationRunner();
            RunLog log = runner.run(popModel, generatorFactory, crossover, parentSelection, selection, ss, problem, rng, stopConditions, observers, logEveryIterations);

            checkCancelled();

            long endTime = System.nanoTime();
            double runtimeMs = (endTime - startTime) / 1_000_000.0;

            List<Integer> evaluations = log.getEvaluations();
            int finalEvaluations = evaluations.isEmpty() ? 0 : evaluations.getLast();

            perProblemRuns.add(ViewMapper.toRunResponse(
                ss.id(),
                pid,
                log.getIterations(),
                evaluations,
                log.getSeries(),
                runtimeMs,
                finalEvaluations
            ));
        }

        return ViewMapper.toRunGroupResponse(runIndex, runSeed, perProblemRuns);
    }
}