package dk.dtu.scout.backend.service;

import dk.dtu.scout.ScoutComponent;
import dk.dtu.scout.selection.SelectionRule;
import dk.dtu.scout.backend.exception.BadRequestException;
import dk.dtu.scout.backend.instance.InstanceMapper;
import dk.dtu.scout.crossover.Crossover;
import dk.dtu.scout.generator.Generator;
import dk.dtu.scout.observer.Observer;
import dk.dtu.scout.parentSelectionRule.ParentSelectionRule;
import dk.dtu.scout.population.PopulationModel;
import dk.dtu.scout.problems.Problem;
import dk.dtu.scout.searchSpace.SearchSpace;
import dk.dtu.scout.stopcondition.StopCondition;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Creates configured Scout components for run execution.
 * Run requests contain component ids and parameter maps. This factory resolves those ids
 * through ComponentRegistry instances, applies the provided parameters, and performs
 * compatibility checks that depend on the selected search space or problem.
 *
 * @author s235257 & Ahmed
 */
@Service
public class RunComponentFactory {

    private final ComponentRegistry<Generator<?>> generatorRegistry;
    private final ComponentRegistry<SelectionRule<?>> selectionRegistry;
    private final ComponentRegistry<PopulationModel<?>> populationModelRegistry;
    private final ComponentRegistry<Problem<?>> problemRegistry;
    private final ComponentRegistry<SearchSpace<?>> searchSpaceRegistry;
    private final ComponentRegistry<ParentSelectionRule<?>> parentSelectionRegistry;
    private final ComponentRegistry<Crossover<?>> crossoverRegistry;
    private final ComponentRegistry<StopCondition<?>> stopConditionRegistry;
    private final ComponentRegistry<Observer<?>> observerRegistry;

    public RunComponentFactory(
        ComponentRegistry<Generator<?>> generatorRegistry,
        ComponentRegistry<SelectionRule<?>> selectionRegistry,
        ComponentRegistry<PopulationModel<?>> populationModelRegistry,
        ComponentRegistry<Problem<?>> problemRegistry,
        ComponentRegistry<SearchSpace<?>> searchSpaceRegistry,
        ComponentRegistry<ParentSelectionRule<?>> parentSelectionRegistry,
        ComponentRegistry<Crossover<?>> crossoverRegistry,
        ComponentRegistry<StopCondition<?>> stopConditionRegistry,
        ComponentRegistry<Observer<?>> observerRegistry
    ) {
        this.generatorRegistry = generatorRegistry;
        this.selectionRegistry = selectionRegistry;
        this.populationModelRegistry = populationModelRegistry;
        this.problemRegistry = problemRegistry;
        this.searchSpaceRegistry = searchSpaceRegistry;
        this.parentSelectionRegistry = parentSelectionRegistry;
        this.crossoverRegistry = crossoverRegistry;
        this.stopConditionRegistry = stopConditionRegistry;
        this.observerRegistry = observerRegistry;
    }

    /**
     * Creates a component from a registry and applies the provided parameter map.
     * @param registry the registry containing the component category
     * @param id the selected component id
     * @param componentType the human-readable component type used in error messages
     * @param params the parameter map passed to the component
     * @return the configured component
     * @param <T> the component interface type
     */
    private <T extends ScoutComponent> T createAndConfigure(ComponentRegistry<? extends T> registry, String id, String componentType, Map<String, Object> params) {
        if (id == null || id.isBlank()) {
            throw new BadRequestException(componentType + " must be specified");
        }

        T component = registry.create(id);
        component.configure(params != null ? params : Map.of());
        return component;
    }

    /**
     * Creates and configures the selected search space.
     * @param id the selected search space id
     * @param params search space parameters
     * @return configured search space
     * @param <S> the solution representation type
     */
    @SuppressWarnings("unchecked")
    public <S> SearchSpace<S> createSearchSpace(String id, Map<String, Object> params) {
        return (SearchSpace<S>) createAndConfigure(searchSpaceRegistry, id, "Search space", params);
    }

    /**
     * Creates and configures the selected problem.
     * The search space dimension is always passed as parameter n.
     * @param id the selected problem id
     * @param n the search space dimension
     * @param problemParams problem-specific parameters
     * @return configured problem
     * @param <S> the solution representation type
     */
    @SuppressWarnings("unchecked")
    public <S> Problem<S> createProblem(String id, int n, Map<String, Object> problemParams) {
        Problem<S> problem = (Problem<S>) problemRegistry.create(id);

        Map<String, Object> params = new HashMap<>(problemParams != null ? problemParams : Map.of());
        params.put("n", n);

        prepareProblemSpecificParams(id, params);

        problem.configure(params);
        return problem;
    }

    private void prepareProblemSpecificParams(String problemId, Map<String, Object> params) {
        if ("tsp".equals(problemId)) {
            params.put("tspInstance", InstanceMapper.toTspInstance(asInstanceMap(params.get("tspInstance"), "tspInstance")));
        } else if ("vrp".equals(problemId)) {
            params.put("vrpInstance", InstanceMapper.toVrpInstance(asInstanceMap(params.get("vrpInstance"), "vrpInstance")));
        }
    }

    /**
     * Creates and configures the selected generator.
     * Also validates that the generator supports the selected search space if it declares restrictions.
     * @param id the selected generator id
     * @param params generator parameters
     * @param searchSpaceId the selected search space id
     * @return configured generator
     * @param <S> the solution representation type
     */
    @SuppressWarnings("unchecked")
    public <S> Generator<S> createGenerator(String id, Map<String, Object> params, String searchSpaceId) {
        Generator<S> generator = (Generator<S>) createAndConfigure(generatorRegistry, id, "Generator", params);

        if (!generator.supportedSearchSpaces().isEmpty() && !generator.supportedSearchSpaces().contains(searchSpaceId)) {
            throw new BadRequestException("Generator '" + id + "' does not support search space '" + searchSpaceId + "'");
        }

        return generator;
    }

    /**
     * Creates and configures the selected selection rule.
     * @param id the selected selection rule id
     * @param params selection rule parameters
     * @return configured selection rule
     * @param <S> the solution representation type
     */
    @SuppressWarnings("unchecked")
    public <S> SelectionRule<S> createSelectionRule(String id, Map<String, Object> params) {
        return (SelectionRule<S>) createAndConfigure(selectionRegistry, id, "Selection rule", params);
    }

    /**
     * Creates and configures the selected population model.
     * @param id the selected population model id
     * @param params population model parameters
     * @return configured population model
     * @param <S> the solution representation type
     */
    @SuppressWarnings("unchecked")
    public <S> PopulationModel<S> createPopulationModel(String id, Map<String, Object> params) {
        return (PopulationModel<S>) createAndConfigure(populationModelRegistry, id, "Population model", params);
    }

    /**
     * Creates the selected stop condition chain.
     * All selected stop conditions receive the same parameter map.
     * @param ids selected stop condition ids
     * @param params stop condition parameters
     * @return configured stop condition chain
     * @param <S> the solution representation type
     */
    @SuppressWarnings("unchecked")
    public <S> List<StopCondition<S>> createStopConditionChain(List<String> ids, Map<String, Object> params) {
        if (ids == null || ids.isEmpty()) {
            throw new BadRequestException("Stop condition must be specified");
        }

        Map<String, Object> effectiveParams = params != null ? params : Map.of();
        return ids.stream().map(id -> (StopCondition<S>) createAndConfigure(stopConditionRegistry, id, "Stop condition", effectiveParams)).toList();
    }

    /**
     * Creates the selected observers.
     * If no observers are selected, an empty list is returned.
     * @param ids selected observer ids
     * @param params observer parameters
     * @return configured observers
     * @param <S> the solution representation type
     */
    @SuppressWarnings("unchecked")
    public <S> List<Observer<S>> createObservers(List<String> ids, Map<String, Object> params) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        Map<String, Object> effectiveParams = params != null ? params : Map.of();
        return ids.stream().map(id -> (Observer<S>) createAndConfigure(observerRegistry, id, "Observer", effectiveParams)).toList();
    }

    /**
     * Creates the selected crossover if one was provided.
     * @param id the selected crossover id, or null if no crossover is used
     * @param params crossover parameters
     * @return configured crossover, or null if none is selected
     * @param <S> the solution representation type
     */
    @SuppressWarnings("unchecked")
    public <S> Crossover<S> createOptionalCrossover(String id, Map<String, Object> params) {
        if (id == null || id.isBlank()) {
            return null;
        }
        return (Crossover<S>) createAndConfigure(crossoverRegistry, id, "Crossover", params);
    }

    /**
     * Creates and configures the selected parent selection rule.
     * @param id the selected parent selection rule id
     * @param params parent selection rule parameters
     * @return configured parent selection rule
     * @param <S> the solution representation type
     */
    @SuppressWarnings("unchecked")
    public <S> ParentSelectionRule<S> createParentSelectionRule(String id, Map<String, Object> params) {
        return (ParentSelectionRule<S>) createAndConfigure(parentSelectionRegistry, id, "Parent selection rule", params);
    }

    /**
     * Validates that the selected problem supports the selected search space.
     * @param problem the configured problem
     * @param problemId the selected problem id
     * @param searchSpaceId the selected search space id
     * @param <S> the solution representation type
     */
    public <S> void validateProblemSearchSpaceCompatibility(Problem<S> problem, String problemId, String searchSpaceId) {
        if (!problem.supportedSearchSpaces().isEmpty() && !problem.supportedSearchSpaces().contains(searchSpaceId)) {
            throw new BadRequestException("Problem '" + problemId + "' does not support search space '" + searchSpaceId + "'");
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