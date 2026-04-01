package dk.dtu.scout.backend.service;

import dk.dtu.scout.acceptance.SelectionRule;
import dk.dtu.scout.backend.exception.BadRequestException;
import dk.dtu.scout.crossover.Crossover;
import dk.dtu.scout.generator.Generator;
import dk.dtu.scout.observer.FitnessObserver;
import dk.dtu.scout.observer.Observer;
import dk.dtu.scout.parentSelectionRule.ParentSelectionRule;
import dk.dtu.scout.population.PopulationModel;
import dk.dtu.scout.problems.Problem;
import dk.dtu.scout.searchSpace.SearchSpace;
import dk.dtu.scout.stopcondition.MaxIterations;
import dk.dtu.scout.stopcondition.StopCondition;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Creates and configures components from registries, enforcing compatibility rules.
 */
@Service
@SuppressWarnings({"rawtypes", "unchecked"})
public class RunComponentFactory {

    private final ComponentRegistry<Generator> mutationRegistry;
    private final ComponentRegistry<SelectionRule> selectionRegistry;
    private final ComponentRegistry<PopulationModel> populationModelRegistry;
    private final ComponentRegistry<Problem> problemRegistry;
    private final ComponentRegistry<SearchSpace> searchSpaceRegistry;
    private final ComponentRegistry<ParentSelectionRule> parentSelectionRegistry;
    private final ComponentRegistry<Crossover> crossoverRegistry;
    private final ComponentRegistry<StopCondition> stopConditionRegistry;
    private final ComponentRegistry<Observer> observerRegistry;

    public RunComponentFactory(
            ComponentRegistry<Generator> mutationRegistry,
            ComponentRegistry<SelectionRule> selectionRegistry,
            ComponentRegistry<PopulationModel> populationModelRegistry,
            ComponentRegistry<Problem> problemRegistry,
            ComponentRegistry<SearchSpace> searchSpaceRegistry,
            ComponentRegistry<ParentSelectionRule> parentSelectionRegistry,
            ComponentRegistry<Crossover> crossoverRegistry,
            ComponentRegistry<StopCondition> stopConditionRegistry,
            ComponentRegistry<Observer> observerRegistry
    ) {
        this.mutationRegistry = mutationRegistry;
        this.selectionRegistry = selectionRegistry;
        this.populationModelRegistry = populationModelRegistry;
        this.problemRegistry = problemRegistry;
        this.searchSpaceRegistry = searchSpaceRegistry;
        this.parentSelectionRegistry = parentSelectionRegistry;
        this.crossoverRegistry = crossoverRegistry;
        this.stopConditionRegistry = stopConditionRegistry;
        this.observerRegistry = observerRegistry;
    }

    private <T> T createAndConfigure(
            ComponentRegistry<?> registry,
            List<String> ids,
            String componentType,
            Map<String, Object> params
    ) {
        if (ids == null || ids.isEmpty()) {
            throw new BadRequestException(componentType + " must be specified");
        }

        Object component = registry.create(ids.getFirst());

        try {
            component.getClass()
                .getMethod("configure", Map.class)
                .invoke(component, params != null ? params : Map.of());
        } catch (Exception e) {
            throw new RuntimeException("Failed to configure component: " + component.getClass().getSimpleName(), e);
        }
        return (T) component;
    }

    public <S> SearchSpace<S> createSearchSpace(List<String> ids, Map<String, Object> params) {
        return createAndConfigure(searchSpaceRegistry, ids, "Search space", params);
    }

    public <S> Problem<S> createProblem(String id, int n, Map<String, Object> problemParams) {
        Problem<S> problem = (Problem<S>) problemRegistry.create(id);

        Map<String, Object> params = new HashMap<>(Map.of("n", n));
        if (problemParams != null) {
            params.putAll(problemParams);
        }

        problem.configure(params);
        return problem;
    }

    public <S> Generator<S> createGenerator(List<String> ids, Map<String, Object> params, String searchSpaceId) {
        Generator<S> generator = createAndConfigure(mutationRegistry, ids, "Generator", params);

        if (!generator.supportedSearchSpaces().isEmpty() &&
                !generator.supportedSearchSpaces().contains(searchSpaceId)) {
            throw new BadRequestException(
                    "Generator '" + ids.getFirst() + "' does not support search space '" + searchSpaceId + "'"
            );
        }
        return generator;
    }

    public SelectionRule createSelectionRule(List<String> ids, Map<String, Object> params) {
        return createAndConfigure(selectionRegistry, ids, "Selection rule", params);
    }

    public <S> PopulationModel<S> createPopulationModel(List<String> ids, Map<String, Object> params) {
        return createAndConfigure(populationModelRegistry, ids, "Population model", params);
    }

    public <S> List<StopCondition<S>> createStopConditionChain(List<String> ids, Map<String, Object> params) {
        if (ids == null || ids.isEmpty()) return List.of(new MaxIterations<>());
        final Map<String, Object> p = (params == null) ? Map.of() : params;
        return ids.stream().map(id -> (StopCondition<S>) createAndConfigure(stopConditionRegistry, List.of(id), "Stop condition", p)).toList();
    }

    public <S> List<Observer<S>> createObservers(List<String> ids, Map<String, Object> params) {
        if (ids == null || ids.isEmpty()) return List.of(new FitnessObserver<>());
        final Map<String, Object> p = (params == null) ? Map.of() : params;
        return ids.stream().map(id -> (Observer<S>) createAndConfigure(observerRegistry, List.of(id), "Observer", p)).toList();
    }

    public <S> Crossover<S> createOptionalCrossover(List<String> ids, Map<String, Object> params) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        return createAndConfigure(crossoverRegistry, ids, "Crossover", params);
    }

    public <S> ParentSelectionRule<S> createParentSelectionRule(List<String> ids, Map<String, Object> params) {
        if (ids == null || ids.isEmpty()) {
            return createAndConfigure(
                    parentSelectionRegistry,
                    List.of("random-parents"),
                    "Parent selection rule",
                    Map.of()
            );
        }

        return createAndConfigure(parentSelectionRegistry, ids, "Parent selection rule", params);
    }

    public <S> void validateProblemSearchSpaceCompatibility(Problem<S> problem, String problemId, String searchSpaceId) {
        if (!problem.supportedSearchSpaces().isEmpty() &&
                !problem.supportedSearchSpaces().contains(searchSpaceId)) {
            throw new BadRequestException(
                    "Problem '" + problemId + "' does not support search space '" + searchSpaceId + "'"
            );
        }
    }
}
