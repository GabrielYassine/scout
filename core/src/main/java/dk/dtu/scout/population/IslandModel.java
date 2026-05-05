package dk.dtu.scout.population;

import dk.dtu.scout.ScoutComponent;
import dk.dtu.scout.State;
import dk.dtu.scout.datatypes.StateKeys;
import dk.dtu.scout.dto.EvaluatedSolution;
import dk.dtu.scout.dto.Parameter;
import dk.dtu.scout.generator.Generator;
import dk.dtu.scout.logging.IterationSnapshot;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
/**
 * Island-based population model.
 *
 * The model splits the population into multiple islands.
 * Each island keeps its own parent population, creates offspring locally,
 * and occasionally exchanges good solutions with other islands.
 *
 * This is similar to a Mu-Lambda population model, but divided into
 * several semi-independent subpopulations.
 */
@Component
@Scope("prototype")
public class IslandModel<S> implements PopulationModel<S> {
    //  Default parameters for the island model. These can be overridden via configuration.
    private int numIslands = 4;
    private int mu = 1;          // parents per island
    private int lambda = 1;      // offspring per island
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
        return "Implements multiple subpopulations. Each island evolves its own parents and offspring. After each epoch, good solutions migrate between islands.";
    }

    @Override
    public List<Parameter> params() {
        return List.of(
                new Parameter("numIslands", "Number of islands", "int", numIslands, 1.0, null, null),
                new Parameter("mu", "μ (parents per island)", "int", mu, 1.0, null, null),
                new Parameter("lambda", "λ (offspring per island)", "int", lambda, 1.0, null, null),
                new Parameter("epochLength", "Epoch length (iterations)", "int", epochLength, 1.0, null, null)
        );
    }

    @Override
    public void configure(Map<String, Object> params) {
        if (params == null) {
            return;
        }
        this.numIslands = positiveIntParam(params, "numIslands", numIslands, "Number of islands must be positive");
        this.mu = positiveIntParam(params, "mu", mu, "Mu must be positive");
        this.lambda = positiveIntParam(params, "lambda", lambda, "Lambda must be positive");
        this.epochLength = positiveIntParam(params, "epochLength", epochLength, "Epoch length must be positive");
    }

    // State of the entire island model, containing the state of each individual island.
    private static final class IslandModelState<S> implements PopulationState<S> {
        private final List<IslandState<S>> islands;

        private IslandModelState(List<IslandState<S>> islands) {
            this.islands = islands;
        }
    }
    // State of a single island, including its current parents, best solution, generator state, and random number generator.
    private static final class IslandState<S> {
        List<EvaluatedSolution<S>> parentsEvaluated;
        S current;
        double currentFitness;
        S best;
        double bestFitness;
        final Generator<S> generator;
        final State state;
        final Random rng;
        final List<ScoutComponent> components;
        IslandState(
                List<EvaluatedSolution<S>> parentsEvaluated,
                S current,
                double currentFitness,
                S best,
                double bestFitness,
                Generator<S> generator,
                State state,
                Random rng,
                List<ScoutComponent> components
        ) {
            this.parentsEvaluated = parentsEvaluated;
            this.current = current;
            this.currentFitness = currentFitness;
            this.best = best;
            this.bestFitness = bestFitness;
            this.generator = generator;
            this.state = state;
            this.rng = rng;
            this.components = components;
        }
    }
    // Initializes the island model by creating the specified number of islands, each with its own random generator and initial parent population.
    @Override
    public PopulationInitialization<S> initialize(PopulationModelContext<S> context) {
        List<IslandState<S>> islands = new ArrayList<>(numIslands);

        for (int i = 0; i < numIslands; i++) {
            Random islandRng = new Random(context.rng().nextLong());
            Generator<S> islandGenerator = context.generatorFactory().get();
            State islandState = new State();
            islandState.update(Map.of(
                    StateKeys.PROBLEM, context.problem(),
                    StateKeys.DIMENSION, context.space().dimension(),
                    StateKeys.SEARCH_SPACE_ID, context.space().id()
            ));
            islandGenerator.init(islandState);

            List<ScoutComponent> components = List.of(islandGenerator);

            List<EvaluatedSolution<S>> parentsEvaluated = new ArrayList<>(mu);

            for (int j = 0; j < mu; j++) {

                S parent = context.space().randomSolution(islandRng);
                double parentFitness = context.problem().fitness(parent);

                parentsEvaluated.add(new EvaluatedSolution<>(parent, parentFitness));
            }

            EvaluatedSolution<S> representative = bestOf(parentsEvaluated);

            S current = representative.value();
            double currentFitness = representative.fitness();

            S best = current;
            double bestFitness = currentFitness;

            IslandState<S> island = new IslandState<>(
                    parentsEvaluated,
                    current,
                    currentFitness,
                    best,
                    bestFitness,
                    islandGenerator,
                    islandState,
                    islandRng,
                    components
            );
            islands.add(island);
        }

        IslandState<S> globalCurrent = globalBestCurrentIsland(islands);
        IslandState<S> globalBest = globalBestEverIsland(islands);
        int evaluations = numIslands * mu;
        int iteration = 0;

        IterationSnapshot<S> initial = new IterationSnapshot<>(
                iteration,
                evaluations,
                new EvaluatedSolution<>(globalCurrent.current, globalCurrent.currentFitness),
                new EvaluatedSolution<>(globalBest.best, globalBest.bestFitness),
                true
        );
        Map<String, Object> stateVariables = Map.of(
                StateKeys.CURRENT, globalCurrent.current,
                StateKeys.CURRENT_FITNESS, globalCurrent.currentFitness,
                StateKeys.BEST, globalBest.best,
                StateKeys.BEST_FITNESS, globalBest.bestFitness
        );

        return new PopulationInitialization<>(new IslandModelState<>(islands), initial, evaluations, stateVariables, List.of());
    }
    // Performs one iteration of the island model.
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
        // For each island, perform selection, crossover, and mutation to create offspring, then select the next generation of parents.
        for (int i = 0; i < numIslands; i++) {
            IslandState<S> island = islands.get(i);
            Random islandRng = island.rng;

            updateIslandStateBeforeStep(island);
            updateComponentStateVariables(island.state, island.components);

            List<EvaluatedSolution<S>> generationEvaluated = new ArrayList<>(lambda);
            // For each offspring to be generated on this island, select parents, perform crossover if applicable, generate the offspring, and evaluate it.
            for (int k = 0; k < lambda; k++) {
                List<EvaluatedSolution<S>> chosenParents = context.parentSelection().selectParents(island.parentsEvaluated, islandRng);

                if (chosenParents == null || chosenParents.size() < 2) {
                    throw new IllegalStateException("Parent selection returned fewer than 2 parents");
                }

                S parent1 = chosenParents.get(0).value();
                S parent2 = chosenParents.get(1).value();

                island.state.update(Map.of(StateKeys.SELECTED_PARENT_1, parent1, StateKeys.SELECTED_PARENT_2, parent2));

                context.sharedState().update(Map.of(StateKeys.SELECTED_PARENT_1, parent1, StateKeys.SELECTED_PARENT_2, parent2));

                S offspringBase;

                if (context.crossover() != null) {
                    offspringBase = context.crossover().crossover(islandRng);
                } else {
                    offspringBase = parent1;
                }

                island.state.update(Map.of(StateKeys.OFFSPRING_BASE, offspringBase));

                context.sharedState().update(Map.of(StateKeys.OFFSPRING_BASE, offspringBase));

                S child = island.generator.generate(islandRng);
                double childFitness = context.problem().fitness(child);

                generationEvaluated.add(new EvaluatedSolution<>(child, childFitness));
                evaluationsDelta++;
            }
            // After generating and evaluating the offspring, select the next generation of parents for this island using the selection rule.
            S previousCurrent = island.current;

            List<EvaluatedSolution<S>> nextParentsEvaluated = context.selection().select(
                    island.parentsEvaluated,
                    generationEvaluated,
                    mu,
                    iteration,
                    islandRng
            );

            if (nextParentsEvaluated == null || nextParentsEvaluated.isEmpty()) {
                throw new IllegalStateException("Selection rule returned no parents");
            }

            if (nextParentsEvaluated.size() != mu) {
                throw new IllegalStateException(
                        "Selection rule returned " + nextParentsEvaluated.size() + " parents, expected " + mu
                );
            }

            island.parentsEvaluated = new ArrayList<>(nextParentsEvaluated);

            EvaluatedSolution<S> representative = bestOf(island.parentsEvaluated);

            island.current = representative.value();
            island.currentFitness = representative.fitness();

            boolean accepted = previousCurrent != island.current;
            anyAccepted |= accepted;

            if (island.currentFitness > island.bestFitness) {
                island.best = island.current;
                island.bestFitness = island.currentFitness;
            }
        }

        if (numIslands > 1 && (iteration + 1) % epochLength == 0) {
            migrateRingBest(islands);
        }

        IslandState<S> globalCurrent = globalBestCurrentIsland(islands);
        IslandState<S> globalBest = globalBestEverIsland(islands);

        int newEvaluations = evaluations + evaluationsDelta;

        IterationSnapshot<S> runState = new IterationSnapshot<>(
                iteration,
                newEvaluations,
                new EvaluatedSolution<>(globalCurrent.current, globalCurrent.currentFitness),
                new EvaluatedSolution<>(globalBest.best, globalBest.bestFitness),
                anyAccepted
        );

        Map<String, Object> stateVariables = Map.of(
                StateKeys.CURRENT, globalCurrent.current,
                StateKeys.CURRENT_FITNESS, globalCurrent.currentFitness,
                StateKeys.BEST, globalBest.best,
                StateKeys.BEST_FITNESS, globalBest.bestFitness
        );

        return new PopulationStepResult<>(runState,  evaluationsDelta, stateVariables);
    }

    private void updateIslandStateBeforeStep(IslandState<S> island) {
        island.state.update(Map.of(
                StateKeys.CURRENT, island.current,
                StateKeys.CURRENT_FITNESS, island.currentFitness,
                StateKeys.BEST, island.best,
                StateKeys.BEST_FITNESS, island.bestFitness,
                StateKeys.PARENTS_EVALUATED, List.copyOf(island.parentsEvaluated)
        ));
    }
    // Updates the state variables of the island's components based on the current state of the island.
    private void updateComponentStateVariables(State state, List<ScoutComponent> components) {
        if (state == null || components == null || components.isEmpty()) {
            return;
        }

        Map<String, Object> combinedStateVariables = new HashMap<>();

        for (ScoutComponent component : components) {
            combinedStateVariables.putAll(component.getStateVariables(state));
        }

        state.update(combinedStateVariables);
    }
    // Migrates the best solution from each island to the next island in a ring topology.
    // Each island receives the best solution from its left neighbor and replaces its worst parent
    private void migrateRingBest(List<IslandState<S>> islands) {
        int n = islands.size();

        List<S> bestSolutions = new ArrayList<>(n);
        List<Double> bestFitnesses = new ArrayList<>(n);

        for (IslandState<S> island : islands) {
            bestSolutions.add(island.best);
            bestFitnesses.add(island.bestFitness);
        }

        for (int i = 0; i < n; i++) {
            int from = (i - 1 + n) % n;

            S immigrant = bestSolutions.get(from);
            double immigrantFitness = bestFitnesses.get(from);

            IslandState<S> destination = islands.get(i);

            replaceWorstParentIfBetter(destination, immigrant, immigrantFitness);
            refreshCurrentAndBest(destination);
        }
    }

    private void replaceWorstParentIfBetter(IslandState<S> island, S immigrant, double immigrantFitness) {
        if (island.parentsEvaluated == null || island.parentsEvaluated.isEmpty()) {
            throw new IllegalStateException("Island has no parents");
        }

        int worstIndex = 0;
        double worstFitness = island.parentsEvaluated.get(0).fitness();

        for (int i = 1; i < island.parentsEvaluated.size(); i++) {
            double candidateFitness = island.parentsEvaluated.get(i).fitness();

            if (candidateFitness < worstFitness) {
                worstFitness = candidateFitness;
                worstIndex = i;
            }
        }

        if (immigrantFitness > worstFitness) {
            island.parentsEvaluated.set(
                    worstIndex,
                    new EvaluatedSolution<>(immigrant, immigrantFitness)
            );
        }
    }
    // After migration, the current solution on the island may have changed if the worst parent was replaced. This method updates the current solution and best solution accordingly.
    private void refreshCurrentAndBest(IslandState<S> island) {
        EvaluatedSolution<S> representative = bestOf(island.parentsEvaluated);

        island.current = representative.value();
        island.currentFitness = representative.fitness();

        if (island.currentFitness > island.bestFitness) {
            island.best = island.current;
            island.bestFitness = island.currentFitness;
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

    private IslandState<S> globalBestCurrentIsland(List<IslandState<S>> islands) {
        if (islands == null || islands.isEmpty()) {
            throw new IllegalStateException("No islands available");
        }

        IslandState<S> best = islands.get(0);

        for (int i = 1; i < islands.size(); i++) {
            if (islands.get(i).currentFitness > best.currentFitness) {
                best = islands.get(i);
            }
        }

        return best;
    }

    private IslandState<S> globalBestEverIsland(List<IslandState<S>> islands) {
        if (islands == null || islands.isEmpty()) {
            throw new IllegalStateException("No islands available");
        }

        IslandState<S> best = islands.get(0);

        for (int i = 1; i < islands.size(); i++) {
            if (islands.get(i).bestFitness > best.bestFitness) {
                best = islands.get(i);
            }
        }

        return best;
    }
    // Utility method to extract a positive integer parameter from the configuration map, with error handling.
    private int positiveIntParam(Map<String, Object> params, String key, int currentValue, String errorMessage) {
        if (!params.containsKey(key)) {
            return currentValue;
        }

        int value = ((Number) params.get(key)).intValue();

        if (value <= 0) {
            throw new IllegalArgumentException(errorMessage);
        }

        return value;
    }

}