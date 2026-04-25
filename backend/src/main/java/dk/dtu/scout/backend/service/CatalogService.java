package dk.dtu.scout.backend.service;

import dk.dtu.scout.ScoutComponent;
import dk.dtu.scout.acceptance.SelectionRule;
import dk.dtu.scout.backend.dto.catalog.ComponentDef;
import dk.dtu.scout.backend.util.ViewMapper;
import dk.dtu.scout.crossover.Crossover;
import dk.dtu.scout.generator.Generator;
import dk.dtu.scout.observer.Observer;
import dk.dtu.scout.parentSelectionRule.ParentSelectionRule;
import dk.dtu.scout.population.PopulationModel;
import dk.dtu.scout.problems.Problem;
import dk.dtu.scout.searchSpace.SearchSpace;
import dk.dtu.scout.stopcondition.StopCondition;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CatalogService {

    private final List<SearchSpace<?>> searchSpaces;
    private final List<Problem<?>> problems;
    private final List<Generator<?>> generators;
    private final List<SelectionRule> selectionRules;
    private final List<PopulationModel<?>> populationModels;
    private final List<ParentSelectionRule> parentSelectionRules;
    private final List<Crossover> crossovers;
    private final List<StopCondition<?>> stopConditions;
    private final List<Observer<?>> observers;

    public CatalogService(
        List<SearchSpace<?>> searchSpaces,
        List<Problem<?>> problems,
        List<Generator<?>> generators,
        List<SelectionRule> selectionRules,
        List<PopulationModel<?>> populationModels,
        List<ParentSelectionRule> parentSelectionRules,
        List<Crossover> crossovers,
        List<StopCondition<?>> stopConditions,
        List<Observer<?>> observers
    ) {
        this.searchSpaces = searchSpaces;
        this.problems = problems;
        this.generators = generators;
        this.selectionRules = selectionRules;
        this.populationModels = populationModels;
        this.parentSelectionRules = parentSelectionRules;
        this.crossovers = crossovers;
        this.stopConditions = stopConditions;
        this.observers = observers;
    }

    /**
     * Generic helper to convert a list of components to a list of component defs,
     * which only contain the information needed for the frontend to display them in the selector tabs.
     * Knows the component list by spring injection, and is used by the individual endpoint methods to convert the components to component defs.
     * @param type The type of the components, e.g. "problem" or "crossover".
     * @param components The components to convert.
     * @return A list of component defs containing the information needed for the frontend to display the components in the selector tabs.
     * @param <T> The type of the components, which must extend ScoutComponent to be convertible to ComponentDef.
     */
    private <T extends ScoutComponent> List<ComponentDef> toComponentDefs(String type, List<T> components) {
        return components.stream().map(component -> ViewMapper.toComponentDef(type, component)).toList();
    }

    public List<ComponentDef> searchSpaces() {
        return toComponentDefs("searchSpace", searchSpaces);
    }

    public List<ComponentDef> problems() {
        return toComponentDefs("problem", problems);
    }

    public List<ComponentDef> generators() {
        return toComponentDefs("generator", generators);
    }

    public List<ComponentDef> selectionRules() {
        return toComponentDefs("selectionRule", selectionRules);
    }

    public List<ComponentDef> populationModels() {
        return toComponentDefs("populationModel", populationModels);
    }

    public List<ComponentDef> parentSelectionRules() {
        return toComponentDefs("parentSelectionRule", parentSelectionRules);
    }

    public List<ComponentDef> crossovers() {
        return toComponentDefs("crossover", crossovers);
    }

    public List<ComponentDef> stopConditions() {
        return toComponentDefs("stopCondition", stopConditions);
    }

    public List<ComponentDef> observers() {
        return toComponentDefs("observer", observers);
    }
}