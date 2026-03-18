package dk.dtu.scout.backend.service;

import java.util.List;

import dk.dtu.scout.backend.dto.catalog.*;

import dk.dtu.scout.Component;
import dk.dtu.scout.Parameter;

import dk.dtu.scout.acceptance.AcceptanceRule;
import dk.dtu.scout.construction.ConstructionPolicy;
import dk.dtu.scout.heuristic.HeuristicFunction;
import dk.dtu.scout.mutation.Generator;
import dk.dtu.scout.observer.Observer;
import dk.dtu.scout.pheromone.PheromoneModel;
import dk.dtu.scout.population.PopulationModel;
import dk.dtu.scout.problems.Problem;
import dk.dtu.scout.searchSpace.SearchSpace;
import dk.dtu.scout.stopcondition.StopCondition;

import org.springframework.stereotype.Service;

@Service
public class CatalogService {

    private final List<SearchSpace<?>> searchSpaces;
    private final List<Problem<?>> problems;
    private final List<HeuristicFunction<?>> heuristicFunctions;
    private final List<ConstructionPolicy<?>> constructionPolicies;
    private final List<PheromoneModel<?>> pheromoneModels;
    private final List<Generator<?>> generators;
    private final List<AcceptanceRule> acceptanceRules;
    private final List<PopulationModel<?>> populationModels;
    private final List<StopCondition<?>> stopConditions;
    private final List<Observer<?>> observers;

    public CatalogService(
        List<SearchSpace<?>> searchSpaces,
        List<Problem<?>> problems,
        List<HeuristicFunction<?>> heuristicFunctions,
        List<ConstructionPolicy<?>> constructionPolicies,
        List<PheromoneModel<?>> pheromoneModels,
        List<Generator<?>> generators,
        List<AcceptanceRule> acceptanceRules,
        List<PopulationModel<?>> populationModels,
        List<StopCondition<?>> stopConditions,
        List<Observer<?>> observers
        ) {
        this.searchSpaces = searchSpaces;
        this.problems = problems;
        this.heuristicFunctions = heuristicFunctions;
        this.constructionPolicies = constructionPolicies;
        this.pheromoneModels = pheromoneModels;
        this.generators = generators;
        this.acceptanceRules = acceptanceRules;
        this.populationModels = populationModels;
        this.stopConditions = stopConditions;
        this.observers = observers;
    }

    private static ComponentDef toComponentDef(String kind, Component c) {
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

    public List<ComponentDef> heuristicFunctions() {
        return heuristicFunctions.stream().map(c -> toComponentDef("heuristicFunction", c)).toList();
    }

    public List<ComponentDef> constructionPolicies() {
        return constructionPolicies.stream().map(c -> toComponentDef("constructionPolicy", c)).toList();
    }

    public List<ComponentDef> pheromoneModels() {
        return pheromoneModels.stream().map(c -> toComponentDef("pheromoneModel", c)).toList();
    }


    public List<ComponentDef> mutations() {
        return generators.stream().map(c -> toComponentDef("mutation", c)).toList();
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

    public List<AlgorithmDef> algorithmTypes() {
        return List.of(
            new AlgorithmDef(
                "variation",
                "Variation",
                "Variation algorithm with mutation and acceptance rules",
                List.of("searchSpace", "problem", "mutation", "acceptance", "populationModel", "stopCondition", "observer")
            ),
            new AlgorithmDef(
                "construction",
                "Construction",
                "Construction algorithm using pheromone and heuristic guidance",
                List.of("searchSpace", "problem", "constructionPolicy", "pheromoneModel", "heuristicFunction", "stopCondition", "observer")
            )
        );
    }
}