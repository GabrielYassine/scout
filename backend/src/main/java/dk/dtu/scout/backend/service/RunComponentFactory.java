package dk.dtu.scout.backend.service;

import dk.dtu.scout.ScoutComponent;
import dk.dtu.scout.acceptance.SelectionRule;
import dk.dtu.scout.backend.exception.BadRequestException;
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

    private <T extends ScoutComponent> T createAndConfigure(
            ComponentRegistry<T> registry,
            String id,
            String componentType,
            Map<String, Object> params
    ) {
        if (id == null || id.isBlank()) {
            throw new BadRequestException(componentType + " must be specified");
        }

        T component = registry.create(id);
        component.configure(params != null ? params : Map.of());
        return component;
    }

    private <T extends ScoutComponent> T createAndConfigure(
            ComponentRegistry<T> registry,
            List<String> ids,
            String componentType,
            Map<String, Object> params
    ) {
        if (ids == null || ids.isEmpty()) {
            throw new BadRequestException(componentType + " must be specified");
        }

        T component = registry.create(ids.getFirst());
        component.configure(params != null ? params : Map.of());
        return component;
    }

    public <S> SearchSpace<S> createSearchSpace(String id, Map<String, Object> params) {
        return createAndConfigure(searchSpaceRegistry, id, "Search space", params);
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

    public <S> Generator<S> createGenerator(String id, Map<String, Object> params, String searchSpaceId) {
        Generator<S> generator = createAndConfigure(mutationRegistry, id, "Generator", params);

        if (!generator.supportedSearchSpaces().isEmpty() &&
                !generator.supportedSearchSpaces().contains(searchSpaceId)) {
            throw new BadRequestException(
                    "Generator '" + id + "' does not support search space '" + searchSpaceId + "'"
            );
        }
        return generator;
    }

    public SelectionRule createSelectionRule(String id, Map<String, Object> params) {
        return createAndConfigure(selectionRegistry, id, "Selection rule", params);
    }

    public <S> PopulationModel<S> createPopulationModel(String id, Map<String, Object> params) {
        return createAndConfigure(populationModelRegistry, id, "Population model", params);
    }

    public <S> List<StopCondition<S>> createStopConditionChain(List<String> ids, Map<String, Object> params) {
        if (ids == null || ids.isEmpty()) {
            throw new BadRequestException("Stop condition must be specified");
        }
        final Map<String, Object> p = (params == null) ? Map.of() : params;
        return ids.stream()
                .map(id -> (StopCondition<S>) createAndConfigure(stopConditionRegistry, List.of(id), "Stop condition", p))
                .toList();
    }

    public <S> List<Observer<S>> createObservers(List<String> ids, Map<String, Object> params) {
        if (ids == null || ids.isEmpty()) return List.of();
        final Map<String, Object> p = (params == null) ? Map.of() : params;
        return ids.stream()
                .map(id -> (Observer<S>) createAndConfigure(observerRegistry, List.of(id), "Observer", p))
                .toList();
    }

    public <S> Crossover<S> createOptionalCrossover(String id, Map<String, Object> params) {
        if (id == null || id.isBlank()) {
            return null;
        }
        return createAndConfigure(crossoverRegistry, id, "Crossover", params);
    }

    public <S> ParentSelectionRule<S> createParentSelectionRule(String id, Map<String, Object> params) {
        if (id == null || id.isBlank()) {
            throw new BadRequestException("Parent selection rule must be specified");
        }

        return createAndConfigure(parentSelectionRegistry, id, "Parent selection rule", params);
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
