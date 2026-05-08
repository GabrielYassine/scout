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
 * Population model based on multiple semi-independent islands.
 * Each island maintains its own parent population, generator, state, and random
 * number generator. During each iteration, every island creates lambda offspring
 * from its local parent population and then applies the configured selection rule
 * to choose the next parents.
 * After each epoch, the best solution from each island migrates to the next island
 * in a ring topology. Only solutions and fitness values are migrated; generator
 * state such as pheromone vectors or matrices remains local to each island.
 * @param <S> solution representation type
 * @author s230632
 */
@Component
@Scope("prototype")
public class IslandModel<S> implements PopulationModel<S> {

    private int numIslands = 4;
    private int mu = 1;
    private int lambda = 1;
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
        return "Population model with multiple semi-independent subpopulations and ring migration";
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
        numIslands = positiveIntParam(params, "numIslands", numIslands, "Number of islands must be positive");
        mu = positiveIntParam(params, "mu", mu, "Mu must be positive");
        lambda = positiveIntParam(params, "lambda", lambda, "Lambda must be positive");
        epochLength = positiveIntParam(params, "epochLength", epochLength, "Epoch length must be positive");
    }

    private static final class IslandModelState<S> implements PopulationState<S> {
        private final List<IslandState<S>> islands;

        private IslandModelState(List<IslandState<S>> islands) {
            this.islands = islands;
        }
    }

    private static final class IslandState<S> {
        private List<EvaluatedSolution<S>> parentsEvaluated;
        private S current;
        private double currentFitness;
        private S best;
        private double bestFitness;
        private final Generator<S> generator;
        private final State state;
        private final Random rng;
        private final List<ScoutComponent> components;
        private Map<String, Object> componentStateVariables = Map.of();

        private IslandState(
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

    /**
     * Initializes all islands and creates the initial public run snapshot.
     * Each island receives its own random number generator, local state, generator,
     * and initial parent population.
     * @param context shared population model context
     * @return initialized island model state and initial run snapshot
     */
    @Override
    public PopulationInitialization<S> initialize(PopulationModelContext<S> context) {
        List<IslandState<S>> islands = new ArrayList<>(numIslands);

        for (int i = 0; i < numIslands; i++) {
            islands.add(createIsland(context));
        }

        IslandState<S> globalCurrent = bestCurrentIsland(islands);
        IslandState<S> globalBest = bestEverIsland(islands);

        int evaluations = numIslands * mu;

        IterationSnapshot<S> initialSnapshot = snapshot(
            0,
            evaluations,
            globalCurrent.current,
            globalCurrent.currentFitness,
            globalBest.best,
            globalBest.bestFitness,
            true
        );

        return new PopulationInitialization<>(
            new IslandModelState<>(islands),
            initialSnapshot,
            evaluations,
            stateVariables(globalCurrent, globalBest),
            List.of()
        );
    }

    /**
     * Performs one island-model iteration. Each island independently generates offspring and updates
     * its local parent population. If the current iteration reaches the epoch length, ring migration
     * is applied. The returned snapshot represents the globally best current island and the globally best solution found so far.
     * @param context shared population model context
     * @param state current island model state
     * @param iteration current iteration number
     * @param evaluations current number of fitness evaluations
     * @return result of one population model step
     */
    @Override
    public PopulationStepResult<S> step(PopulationModelContext<S> context, PopulationState<S> state, int iteration, int evaluations) {
        IslandModelState<S> modelState = (IslandModelState<S>) state;

        boolean accepted = false;
        int evaluationsDelta = 0;

        for (IslandState<S> island : modelState.islands) {
            accepted |= stepIsland(context, island, iteration);
            evaluationsDelta += lambda;
        }

        if (shouldMigrate(iteration)) {
            migrateRingBest(modelState.islands);
        }

        IslandState<S> globalCurrent = bestCurrentIsland(modelState.islands);
        IslandState<S> globalBest = bestEverIsland(modelState.islands);
        int newEvaluations = evaluations + evaluationsDelta;

        IterationSnapshot<S> snapshot = snapshot(
            iteration,
            newEvaluations,
            globalCurrent.current,
            globalCurrent.currentFitness,
            globalBest.best,
            globalBest.bestFitness,
            accepted
        );

        return new PopulationStepResult<>(snapshot, evaluationsDelta, stateVariables(globalCurrent, globalBest));
    }

    /**
     * Creates one island with its own local state, generator, random number generator,
     * and initial parent population.
     * @param context shared population model context
     * @return initialized island state
     */
    private IslandState<S> createIsland(PopulationModelContext<S> context) {
        Random islandRng = new Random(context.rng().nextLong());
        State islandState = new State();

        islandState.update(Map.of(
            StateKeys.PROBLEM, context.problem(),
            StateKeys.DIMENSION, context.space().dimension(),
            StateKeys.SEARCH_SPACE_ID, context.space().id()
        ));

        Generator<S> generator = context.generatorFactory().get();
        generator.init(islandState);

        List<EvaluatedSolution<S>> parentsEvaluated = initializeParents(context, islandRng);
        EvaluatedSolution<S> representative = bestOf(parentsEvaluated);

        return new IslandState<>(
            parentsEvaluated,
            representative.value(),
            representative.fitness(),
            representative.value(),
            representative.fitness(),
            generator,
            islandState,
            islandRng,
            List.of(generator)
        );
    }

    /**
     * Samples and evaluates the initial parent population for one island.
     * @param context shared population model context
     * @param rng island-specific random number generator
     * @return evaluated initial parents
     */
    private List<EvaluatedSolution<S>> initializeParents(PopulationModelContext<S> context, Random rng) {
        List<EvaluatedSolution<S>> parentsEvaluated = new ArrayList<>(mu);

        for (int i = 0; i < mu; i++) {
            S parent = context.space().randomSolution(rng);
            double fitness = context.problem().fitness(parent);
            parentsEvaluated.add(new EvaluatedSolution<>(parent, fitness));
        }

        return parentsEvaluated;
    }

    /**
     * Performs one iteration for a single island.
     * The island first publishes its current state, then generates lambda offspring.
     * After the offspring have been evaluated, generator state variables are updated.
     * @param context shared population model context
     * @param island island to update
     * @param iteration current iteration number
     * @return true if the island's current representative changed
     */
    private boolean stepIsland(PopulationModelContext<S> context, IslandState<S> island, int iteration) {
        updateIslandState(island);

        List<EvaluatedSolution<S>> generationEvaluated = new ArrayList<>(lambda);

        for (int i = 0; i < lambda; i++) {
            generationEvaluated.add(createChild(context, island));
        }

        island.state.update(Map.of(StateKeys.GENERATION_EVALUATED, List.copyOf(generationEvaluated)));

        updateComponentStateVariables(island);

        S previousCurrent = island.current;

        List<EvaluatedSolution<S>> nextParents = context.selection().select(
            island.parentsEvaluated,
            generationEvaluated,
            mu,
            iteration,
            island.rng
        );

        island.parentsEvaluated = new ArrayList<>(nextParents);
        refreshCurrentAndBest(island);

        return previousCurrent != island.current;
    }

    /**
     * Creates and evaluates one offspring for an island.
     * Two parents are selected from the island's local parent population. If crossover
     * is configured, it is used to create the offspring base; otherwise the first
     * parent is used as the offspring base. The generator then creates the final child.
     * @param context shared population model context
     * @param island island generating the child
     * @return evaluated child solution
     */
    private EvaluatedSolution<S> createChild(PopulationModelContext<S> context, IslandState<S> island) {
        List<EvaluatedSolution<S>> selectedParents = context.parentSelection().selectParents(island.parentsEvaluated, island.rng);

        S parent1 = selectedParents.get(0).value();
        S parent2 = selectedParents.get(1).value();

        island.state.update(Map.of(StateKeys.SELECTED_PARENT_1, parent1, StateKeys.SELECTED_PARENT_2, parent2));
        context.sharedState().update(Map.of(StateKeys.SELECTED_PARENT_1, parent1, StateKeys.SELECTED_PARENT_2, parent2));

        S offspringBase = context.crossover() == null ? parent1 : context.crossover().crossover(island.rng);

        island.state.update(Map.of(StateKeys.OFFSPRING_BASE, offspringBase));
        context.sharedState().update(Map.of(StateKeys.OFFSPRING_BASE, offspringBase));

        S child = island.generator.generate(island.rng);
        double childFitness = context.problem().fitness(child);

        return new EvaluatedSolution<>(child, childFitness);
    }

    /**
     * Publishes the island's local state variables before offspring generation.
     * These values allow generators, crossover operators, and other components to
     * access the island's current solution, best solution, and parent population.
     * @param island island whose state should be updated
     */
    private void updateIslandState(IslandState<S> island) {
        island.state.update(Map.of(
            StateKeys.CURRENT, island.current,
            StateKeys.CURRENT_FITNESS, island.currentFitness,
            StateKeys.BEST, island.best,
            StateKeys.BEST_FITNESS, island.bestFitness,
            StateKeys.PARENTS_EVALUATED, List.copyOf(island.parentsEvaluated)
        ));
    }

    /**
     * Collects state variables published by the island's local components.
     * The returned variables are cached on the island so that the global state can
     * expose them without calling component logic again.
     * @param island island whose components should publish state variables
     */
    private void updateComponentStateVariables(IslandState<S> island) {
        Map<String, Object> variables = new HashMap<>();

        for (ScoutComponent component : island.components) {
            variables.putAll(component.getStateVariables(island.state));
        }

        island.state.update(variables);
        island.componentStateVariables = variables;
    }

    private boolean shouldMigrate(int iteration) {
        return numIslands > 1 && (iteration + 1) % epochLength == 0;
    }

    /**
     * Migrates the best solution from each island to the next island in a ring.
     * Island i receives the best solution from island i - 1. The immigrant replaces
     * the worst parent on the destination island only if it has better fitness.
     * @param islands all islands in the model
     */
    private void migrateRingBest(List<IslandState<S>> islands) {
        List<EvaluatedSolution<S>> immigrants = new ArrayList<>(islands.size());

        for (IslandState<S> island : islands) {
            immigrants.add(new EvaluatedSolution<>(island.best, island.bestFitness));
        }

        for (int i = 0; i < islands.size(); i++) {
            int from = (i - 1 + islands.size()) % islands.size();
            replaceWorstParentIfBetter(islands.get(i), immigrants.get(from));
            refreshCurrentAndBest(islands.get(i));
        }
    }

    private void replaceWorstParentIfBetter(IslandState<S> island, EvaluatedSolution<S> immigrant) {
        int worstIndex = worstParentIndex(island.parentsEvaluated);

        if (immigrant.fitness() > island.parentsEvaluated.get(worstIndex).fitness()) {
            island.parentsEvaluated.set(worstIndex, immigrant);
        }
    }

    private int worstParentIndex(List<EvaluatedSolution<S>> parents) {
        int worstIndex = 0;
        double worstFitness = parents.getFirst().fitness();

        for (int i = 1; i < parents.size(); i++) {
            double fitness = parents.get(i).fitness();

            if (fitness < worstFitness) {
                worstFitness = fitness;
                worstIndex = i;
            }
        }

        return worstIndex;
    }

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
        EvaluatedSolution<S> best = evaluatedSolutions.getFirst();

        for (int i = 1; i < evaluatedSolutions.size(); i++) {
            EvaluatedSolution<S> candidate = evaluatedSolutions.get(i);

            if (candidate.fitness() > best.fitness()) {
                best = candidate;
            }
        }

        return best;
    }

    private IslandState<S> bestCurrentIsland(List<IslandState<S>> islands) {
        IslandState<S> best = islands.getFirst();

        for (int i = 1; i < islands.size(); i++) {
            IslandState<S> candidate = islands.get(i);

            if (candidate.currentFitness > best.currentFitness) {
                best = candidate;
            }
        }

        return best;
    }

    private IslandState<S> bestEverIsland(List<IslandState<S>> islands) {
        IslandState<S> best = islands.getFirst();

        for (int i = 1; i < islands.size(); i++) {
            IslandState<S> candidate = islands.get(i);

            if (candidate.bestFitness > best.bestFitness) {
                best = candidate;
            }
        }

        return best;
    }

    private IterationSnapshot<S> snapshot(
        int iteration,
        int evaluations,
        S current,
        double currentFitness,
        S best,
        double bestFitness,
        boolean accepted
    ) {
        return new IterationSnapshot<>(
            iteration,
            evaluations,
            new EvaluatedSolution<>(current, currentFitness),
            new EvaluatedSolution<>(best, bestFitness),
            accepted
        );
    }

    private Map<String, Object> stateVariables(IslandState<S> globalCurrent, IslandState<S> globalBest) {
        Map<String, Object> variables = new HashMap<>();

        variables.put(StateKeys.CURRENT, globalCurrent.current);
        variables.put(StateKeys.CURRENT_FITNESS, globalCurrent.currentFitness);
        variables.put(StateKeys.BEST, globalBest.best);
        variables.put(StateKeys.BEST_FITNESS, globalBest.bestFitness);
        variables.putAll(globalBest.componentStateVariables);

        return variables;
    }

    private int positiveIntParam(
            Map<String, Object> params,
            String key,
            int currentValue,
            String errorMessage
    ) {
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