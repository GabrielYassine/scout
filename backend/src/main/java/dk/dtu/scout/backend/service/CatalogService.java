package dk.dtu.scout.backend.service;

import java.util.List;

import dk.dtu.scout.backend.dto.catalog.*;

import dk.dtu.scout.ScoutComponent;
import dk.dtu.scout.Parameter;

import dk.dtu.scout.acceptance.AcceptanceRule;
import dk.dtu.scout.generator.Generator;
import dk.dtu.scout.observer.Observer;
import dk.dtu.scout.population.PopulationModel;
import dk.dtu.scout.problems.Problem;
import dk.dtu.scout.searchSpace.SearchSpace;
import dk.dtu.scout.stopcondition.StopCondition;

import org.springframework.stereotype.Service;

@Service
public class CatalogService {

    private final List<SearchSpace<?>> searchSpaces;
    private final List<Problem<?>> problems;

    private final List<Generator<?>> generators;
    private final List<AcceptanceRule> acceptanceRules;
    private final List<PopulationModel<?>> populationModels;
    private final List<StopCondition<?>> stopConditions;
    private final List<Observer<?>> observers;

    public CatalogService(
        List<SearchSpace<?>> searchSpaces,
        List<Problem<?>> problems,
        List<Generator<?>> generators,
        List<AcceptanceRule> acceptanceRules,
        List<PopulationModel<?>> populationModels,
        List<StopCondition<?>> stopConditions,
        List<Observer<?>> observers
        ) {
        this.searchSpaces = searchSpaces;
        this.problems = problems;
        this.generators = generators;
        this.acceptanceRules = acceptanceRules;
        this.populationModels = populationModels;
        this.stopConditions = stopConditions;
        this.observers = observers;
    }

    private static ComponentDef toComponentDef(String kind, ScoutComponent c) {
        return new ComponentDef(
            kind,
            c.id(),
            c.displayName(),
            c.description(),
            c.params().stream().map(CatalogService::toParamDef).toList(),
            c.supportedSearchSpaces()
        );
    }

    private static ParamDef toParamDef(Parameter p) {
        return new ParamDef(p.key(), p.label(), p.type(), p.defaultValue(), p.min(), p.max());
    }

    public List<ComponentDef> searchSpaces() {
        return searchSpaces.stream().map(c -> toComponentDef("searchSpace", c)).toList();
    }

    public List<ComponentDef> problems() {
        return problems.stream().map(c -> toComponentDef("problem", c)).toList();
    }

    public List<ComponentDef> generators() {
        return generators.stream().map(c -> toComponentDef("generator", c)).toList();
    }

    public List<ComponentDef> acceptanceRules() {
        return acceptanceRules.stream().map(c -> toComponentDef("acceptanceRule", c)).toList();
    }

    public List<ComponentDef> populationModels() {
        return populationModels.stream().map(c -> toComponentDef("populationModel", c)).toList();
    }

    public List<ComponentDef> stopConditions() {
        return stopConditions.stream().map(c -> toComponentDef("stopCondition", c)).toList();
    }

    public List<ComponentDef> observers() {
        return observers.stream().map(c -> toComponentDef("observer", c)).toList();
    }
}