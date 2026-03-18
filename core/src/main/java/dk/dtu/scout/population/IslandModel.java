package dk.dtu.scout.population;

import dk.dtu.scout.Parameter;
import dk.dtu.scout.acceptance.AcceptanceRule;
import dk.dtu.scout.logging.RunLog;
import dk.dtu.scout.logging.RunState;
import dk.dtu.scout.mutation.Generator;
import dk.dtu.scout.observer.Observer;
import dk.dtu.scout.problems.Problem;
import dk.dtu.scout.searchSpace.SearchSpace;
import dk.dtu.scout.stopcondition.StopCondition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Component
@Scope("prototype")
public class IslandModel<S>  implements PopulationModel<S> {
    private int numIslands = 4;
    private int lambda = 1;        // per island (1+λ)
    private int epochLength = 50;

    @Override
    public String id() {
        return "islands";
    }

    @Override
    public String displayName() {
        return "Island Model";
    }

    @Override
    public String description() {
        return "Implements multiple subpopulations. Each subpolution has isolated evolution. After an epoch individuals will migrate between the subpolutions";
    }

    @Override
    public List<Parameter> params() {
        return List.of(
                new Parameter("numIslands", "Number of islands", "int", numIslands, 1.0, null),
                new Parameter("lambda", "λ (offspring per island)", "int", lambda, 1.0, null),
                new Parameter("epochLength", "Epoch length (iterations)", "int", epochLength, 1.0, null)
        );
    }
    @Override
    public void configure(Map<String, Object> params) {
        if (params == null) return;
        if (params.containsKey("numIslands")) {
            int value = ((Number) params.get("numIslands")).intValue();
            if (value <= 0) {
                throw new IllegalArgumentException("Number of islands must be positive");
            }
            this.numIslands = value;
        }
        if (params.containsKey("lambda")) {
            int value = ((Number) params.get("lambda")).intValue();
            if (value <= 0) {
                throw new IllegalArgumentException("Lambda must be positive");
            }
            this.lambda = value;
        }
        if (params.containsKey("epochLength")) {
            int value = ((Number) params.get("epochLength")).intValue();
            if (value <= 0) {
                throw new IllegalArgumentException("Epoch length must be positive");
            }
            this.epochLength = value;
        }
    }

    @Override
    public RunLog run(
            Generator<S> generator,
            AcceptanceRule acceptance,
            SearchSpace<S> space,
            Problem<S> problem,
            Random rng,
            StopCondition<S> stop,
            Observer<S> observer
    ) {
        RunLog log = new RunLog();

        // setup islands
        List<IslandState<S>> islands = new ArrayList<>(numIslands);
        List<Random> islandRng = new ArrayList<>(numIslands);


        for(int i = 0; i < numIslands; i++) {
            Random r = new Random(rng.nextLong());
            islandRng.add(r);

            S x = space.randomSolution(r);
            double fx = problem.fitness(x);
            islands.add(new IslandState<>(x, fx));
        }
        IslandState<S> global = globalBestIsland(islands);
        int evaluations = numIslands;
        int iteration = 0;

        // Initial state
        RunState<S> initial = new RunState<>(iteration, evaluations, global.current, global.currentFitness, global.best, global.bestFitness, false);
        observer.onStart(initial, log);
        log.tick(initial.iteration(), evaluations);
        observer.onStep(initial, log);

        while (true) {
            global = globalBestIsland(islands);
            if(stop.shouldStop(iteration, evaluations, global.bestFitness, global.best) )break;
            boolean anyAccepted = false;

            for(int i = 0; i < numIslands; i++) {
                IslandState<S> isl = islands.get(i);
                Random r = islandRng.get(i);

                S bestChild = null;
                double bestFit = Double.NEGATIVE_INFINITY;

                for (int k = 0; k < lambda; k++) {
                    S child = generator.generate(isl.current, r);
                    double f = problem.fitness(child);
                    if (f > bestFit) { bestFit = f; bestChild = child; }
                    evaluations++;
                }
                boolean accepted = acceptance.accept(isl.currentFitness, bestFit, iteration, r);
                anyAccepted |= accepted;
                if (accepted) {
                    isl.current = bestChild;
                    isl.currentFitness =bestFit;

                    if (isl.currentFitness > isl.bestFitness) {
                        isl.best = isl.current;
                        isl.bestFitness = isl.currentFitness;
                    }
                }
            }
            if (numIslands > 1 && iteration % epochLength == 0) {
                migrateRingBest(islands);
            }
            global = globalBestIsland(islands);
            RunState<S> state = new RunState<>(iteration, evaluations, global.current, global.currentFitness, global.best, global.bestFitness, anyAccepted);
            log.tick(state.iteration(), state.evaluations());
            observer.onStep(state, log);
            iteration++;
        }

        global= globalBestIsland(islands);
        RunState<S> finalState = new RunState<>(iteration - 1, evaluations, global.current, global.currentFitness, global.best, global.bestFitness, false);
        observer.onEnd(finalState, log);
        return log;
    }

    /**
     * Ring migration of best solution: each island sends its best solution to the next island (i -> i+1), and replaces its current
     * **/
    private void migrateRingBest(List<IslandState<S>> islands) {
        int n = islands.size();
        List<S> bestSolutions = new ArrayList<>(n);
        List<Double> bestFitnesses = new ArrayList<>(n);

        for (IslandState<S> isl : islands) {
            bestSolutions.add(isl.best);
            bestFitnesses.add(isl.bestFitness);
        }
        for (int i = 0; i < n; i++) {
            int from = (i - 1 + n) % n;
            S immigrant = bestSolutions.get(from);
            double immigrantFitness = bestFitnesses.get(from);
            IslandState<S> dest = islands.get(i);
            if (immigrantFitness > dest.currentFitness) {
                dest.current = immigrant;
                dest.currentFitness = immigrantFitness;

                if (dest.currentFitness > dest.bestFitness) {
                    dest.best = dest.current;
                    dest.bestFitness = dest.currentFitness;
                }
            }
        }
    }



    private static final class IslandState<S> {
        S current;
        double currentFitness;

        S best;
        double bestFitness;

        IslandState(S x, double fx) {
            this.current = x;
            this.currentFitness = fx;
            this.best = x;
            this.bestFitness = fx;
        }
    }

    private IslandState<S> globalBestIsland(List<IslandState<S>> islands) {
        IslandState<S> best = islands.get(0);
        for (int i = 1; i < islands.size(); i++) {
            if (islands.get(i).bestFitness > best.bestFitness) best = islands.get(i);
        }
        return best;
    }

}
