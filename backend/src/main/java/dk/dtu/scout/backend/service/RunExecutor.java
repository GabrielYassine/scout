package dk.dtu.scout.backend.service;

import dk.dtu.scout.SimulationRunner;
import dk.dtu.scout.selection.SelectionRule;
import dk.dtu.scout.backend.dto.request.RunRequest;
import dk.dtu.scout.backend.dto.run.RunGroupResponse;
import dk.dtu.scout.backend.dto.run.RunResponse;
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
 * The main method runBatch executes a batch of runs based on the provided RunRequest.
 * This batch can include multiple runtimes and multiple problems.
 * @author s235257 & s230632
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
     * @param logEveryEvaluations Determines how often to log progress for each run.
     * @param wsUpdateEveryEvaluations Determines how often to send WebSocket updates for each run.
     * @return A list of RunGroupResponse objects.
     * @param <S> The solution type used in the search space and problems.
     */
    public <S> List<RunGroupResponse> runBatch(RunRequest request, int logEveryEvaluations, int wsUpdateEveryEvaluations) {
        checkCancelled();

        long baseSeed = request.seed();
        int runtimes = request.runTimes();

        Map<String, Object> searchSpaceParams = copyParams(request.searchSpaceParams());
        Supplier<SearchSpace<S>> searchSpaceFactory = () -> factory.createSearchSpace(request.searchSpaceId(), searchSpaceParams);

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
                        logEveryEvaluations,
                        wsUpdateEveryEvaluations
                    )
                ));
            }

            List<RunGroupResponse> results = collectFinishedRuns(futures);
            return results.stream().sorted(Comparator.comparingInt(RunGroupResponse::runIndex)).toList();
        } catch (RuntimeException ex) {
            cancelAll(futures);
            throw ex;
        }
    }

    /**
     * Executes one runtime. A runtime uses one seed and one search space,
     * while each selected problem is executed sequentially within that runtime.
     * @param request original run request
     * @param runIndex index of the current runtime
     * @param runSeed seed used for the current runtime
     * @param searchSpaceFactory factory for creating the search space
     * @param logEveryEvaluations logging interval
     * @param wsUpdateEveryEvaluations websocket update interval
     * @return grouped run result for this runtime
     * @param <S> solution representation type
     */
    private <S> RunGroupResponse runSingleIndex(
        RunRequest request,
        int runIndex,
        long runSeed,
        Supplier<SearchSpace<S>> searchSpaceFactory,
        int logEveryEvaluations,
        int wsUpdateEveryEvaluations
    ) {
        checkCancelled();

        Random rng = new Random(runSeed);

        SearchSpace<S> searchSpace = searchSpaceFactory.get();
        Supplier<Generator<S>> generatorFactory = () -> factory.createGenerator(request.generatorId(), request.generatorParams(), searchSpace.id());

        List<RunResponse> perProblemRuns = new ArrayList<>();

        for (String problemId : request.problemIds()) {
            checkCancelled();

            Map<String, Object> problemParams = copyParams(request.problemParams());

            Problem<S> problem = factory.createProblem(problemId, searchSpace.dimension(), problemParams);

            factory.validateProblemSearchSpaceCompatibility(problem, problemId, searchSpace.id());

            SelectionRule<S> selection = factory.createSelectionRule(request.selectionRuleId(), request.selectionRuleParams());
            List<StopCondition<S>> stopConditions = factory.createStopConditionChain(request.stopConditionIds(), request.stopConditionParams());
            List<Observer<S>> observers = new ArrayList<>(factory.createObservers(request.observerIds(), request.observerParams()));
            PopulationModel<S> populationModel = factory.createPopulationModel(request.populationModelId(), request.populationModelParams());
            Crossover<S> crossover = factory.createOptionalCrossover(request.crossoverId(), request.crossoverParams());
            ParentSelectionRule<S> parentSelection = factory.createParentSelectionRule(request.parentSelectionRuleId(), request.parentSelectionRuleParams());

            if (wsUpdateEveryEvaluations > 0) {
                observers.add(new RunProgressObserver<>(
                    wsSender,
                    request.runId(),
                    runIndex,
                    runSeed,
                    searchSpace.id(),
                    problemId,
                    wsUpdateEveryEvaluations
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
                logEveryEvaluations
            );

            checkCancelled();

            double runtimeMs = (System.nanoTime() - startTime) / 1_000_000.0;

            List<Integer> evaluations = log.getEvaluations();
            int finalEvaluations = evaluations.getLast();

            perProblemRuns.add(ViewMapper.toRunResponse(
                searchSpace.id(),
                problemId,
                evaluations,
                log.getSeries(),
                runtimeMs,
                finalEvaluations
            ));
        }

        return ViewMapper.toRunGroupResponse(runIndex, runSeed, perProblemRuns);
    }

    /**
     * Collects results from asynchronous run futures.
     * @param futures futures from submitted run tasks
     * @return completed run groups
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

    private void checkCancelled() {
        if (Thread.currentThread().isInterrupted()) {
            throw new CancellationException("Run cancelled");
        }
    }

    private Map<String, Object> copyParams(Map<String, Object> params) {
        return params != null ? new LinkedHashMap<>(params) : new LinkedHashMap<>();
    }


    private void cancelAll(List<Future<RunGroupResponse>> futures) {
        for (Future<RunGroupResponse> future : futures) {
            future.cancel(true);
        }
    }
}