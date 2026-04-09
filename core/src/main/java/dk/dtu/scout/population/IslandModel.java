package dk.dtu.scout.population;

import dk.dtu.scout.dto.EvaluatedSolution;
import dk.dtu.scout.dto.Parameter;
import dk.dtu.scout.ScoutComponent;
import dk.dtu.scout.State;
import dk.dtu.scout.datatypes.StateKeys;
import dk.dtu.scout.generator.Generator;
import dk.dtu.scout.logging.RunState;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Component
@Scope("prototype")
public class IslandModel<S> implements PopulationModel<S> {
    private int numIslands = 4;
    private int lambda = 1; // per island (1+λ)
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

    private EvaluatedSolution<S> bestOf(List<EvaluatedSolution<S>> evaluatedSolutions) {
        if (evaluatedSolutions == null || evaluatedSolutions.isEmpty()) {
            throw new IllegalStateException("No evaluated solutions available");
        }

        EvaluatedSolution<S> best = evaluatedSolutions.get(0);
        for (int i = 1; i < evaluatedSolutions.size(); i++) {
            EvaluatedSolution<S> candidate = evaluatedSolutions.get(i);
            if (candidate.fitness() > best.fitness()) {
                best = candidate;
            }
        }
        return best;
    }

    private static final class IslandModelState<S> implements PopulationState<S> {
        private final List<IslandState<S>> islands;

        private IslandModelState(List<IslandState<S>> islands) {
            this.islands = islands;
        }
    }

    private void updateComponentStateVariables(State state, List<ScoutComponent> components) {
        Map<String, Object> combinedStateVariables = new HashMap<>();
        for (ScoutComponent component : components) {
            combinedStateVariables.putAll(component.getStateVariables(state));
        }
        state.update(combinedStateVariables);
    }

    @Override
    public PopulationInitialization<S> initialize(PopulationModelContext<S> context) {
        List<IslandState<S>> islands = new ArrayList<>(numIslands);

        for (int i = 0; i < numIslands; i++) {
            Random r = new Random(context.rng().nextLong());

            Generator<S> islandGenerator = context.generatorFactory().get();
            State islandState = new State();
            islandState.update(Map.of(
                    StateKeys.PROBLEM, context.problem(),
                    StateKeys.DIMENSION, context.space().dimension(),
                    StateKeys.SEARCH_SPACE_ID, context.space().id()
            ));
            islandGenerator.init(islandState);

            List<ScoutComponent> components = List.of(islandGenerator);

            S x = context.space().randomSolution(r);
            double fx = context.problem().fitness(x);
            islands.add(new IslandState<>(x, fx, islandGenerator, islandState, r, components));
        }

        IslandState<S> global = globalBestIsland(islands);
        int evaluations = numIslands;
        int iteration = 0;

        RunState<S> initial = new RunState<>(iteration, evaluations, global.current, global.currentFitness, global.best, global.bestFitness, false);
        Map<String, Object> stateVariables = Map.of(
                StateKeys.CURRENT, global.current,
                StateKeys.CURRENT_FITNESS, global.currentFitness,
                StateKeys.BEST, global.best,
                StateKeys.BEST_FITNESS, global.bestFitness
        );

        return new PopulationInitialization<>(new IslandModelState<>(islands), initial, evaluations, stateVariables, List.of());
    }

    @Override
    public PopulationStepResult<S> step(
            PopulationModelContext<S> context,
            PopulationState<S> state,
            int iteration,
            int evaluations
    ) {
        IslandModelState<S> modelState = (IslandModelState<S>) state;
        List<IslandState<S>> islands = modelState.islands;

        boolean anyAccepted = false;
        int evaluationsDelta = 0;

        for (int i = 0; i < numIslands; i++) {
            IslandState<S> isl = islands.get(i);
            Random r = isl.rng;

            isl.state.update(Map.of(
                    StateKeys.CURRENT, isl.current,
                    StateKeys.CURRENT_FITNESS, isl.currentFitness,
                    StateKeys.BEST, isl.best,
                    StateKeys.BEST_FITNESS, isl.bestFitness,
                    StateKeys.GENERATION_EVALUATED, isl.previousGenerationEvaluated,
                    StateKeys.ISLAND_INDEX, i
            ));
            updateComponentStateVariables(isl.state, isl.components);

            List<EvaluatedSolution<S>> generationEvaluated = new ArrayList<>();

            for (int k = 0; k < lambda; k++) {
                S child = isl.generator.generate(r);
                isl.state.update(Map.of(
                        StateKeys.OFFSPRING_BASE, isl.current
                ));
                double f = context.problem().fitness(child);
                generationEvaluated.add(new EvaluatedSolution<>(child, f));
                evaluationsDelta++;
            }

            double previousCurrentFitness = isl.currentFitness;

            List<EvaluatedSolution<S>> parentsEvaluated = List.of(
                    new EvaluatedSolution<>(isl.current, isl.currentFitness)
            );

            List<EvaluatedSolution<S>> nextParentsEvaluated = context.selection().select(
                    parentsEvaluated,
                    generationEvaluated,
                    1,
                    iteration,
                    r
            );

            if (nextParentsEvaluated == null || nextParentsEvaluated.isEmpty()) {
                throw new IllegalStateException("Selection rule returned no parents");
            }

            if (nextParentsEvaluated.size() > 1) {
                throw new IllegalStateException("Selection rule returned more parents than 1: " + nextParentsEvaluated.size());
            }

            EvaluatedSolution<S> representative = bestOf(nextParentsEvaluated);
            isl.current = representative.value();
            isl.currentFitness = representative.fitness();

            boolean accepted = isl.currentFitness >= previousCurrentFitness;
            anyAccepted |= accepted;

            if (isl.currentFitness > isl.bestFitness) {
                isl.best = isl.current;
                isl.bestFitness = isl.currentFitness;
            }

            isl.previousGenerationEvaluated = generationEvaluated;
        }

        if (numIslands > 1 && (iteration + 1) % epochLength == 0) {
            migrateRingBest(islands);
        }

        IslandState<S> global = globalBestIsland(islands);
        int newEvaluations = evaluations + evaluationsDelta;

        RunState<S> runState = new RunState<>(
                iteration,
                newEvaluations,
                global.current,
                global.currentFitness,
                global.best,
                global.bestFitness,
                anyAccepted
        );

        Map<String, Object> stateVariables = Map.of(
                StateKeys.CURRENT, global.current,
                StateKeys.CURRENT_FITNESS, global.currentFitness,
                StateKeys.BEST, global.best,
                StateKeys.BEST_FITNESS, global.bestFitness
        );

        return new PopulationStepResult<>(runState, evaluationsDelta, stateVariables);
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

        Generator<S> generator;
        State state;
        Random rng;
        List<ScoutComponent> components;

        List<EvaluatedSolution<S>> previousGenerationEvaluated = new ArrayList<>();

        IslandState(S x, double fx, Generator<S> generator, State state, Random rng, List<ScoutComponent> components) {
            this.current = x;
            this.currentFitness = fx;
            this.best = x;
            this.bestFitness = fx;
            this.generator = generator;
            this.state = state;
            this.rng = rng;
            this.components = components;
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
