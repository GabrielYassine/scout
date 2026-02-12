package dk.dtu.scout.backend.service;

import java.util.List;

import dk.dtu.scout.Component;
import dk.dtu.scout.Parameter;
import dk.dtu.scout.acceptance.ElitistAcceptance;
import dk.dtu.scout.acceptance.SimulatedAnnealingAcceptance;
import dk.dtu.scout.algorithms.OnePlusOneEA;
import dk.dtu.scout.algorithms.SimulatedAnnealing;
import dk.dtu.scout.backend.dto.catalog.*;
import dk.dtu.scout.mutation.BitMutation;
import dk.dtu.scout.observer.AcceptanceRateObserver;
import dk.dtu.scout.observer.FitnessObserver;
import dk.dtu.scout.observer.ImprovementObserver;
import dk.dtu.scout.population.DefaultPopulationModel;
import dk.dtu.scout.problems.LeadingOnesProblem;
import dk.dtu.scout.problems.OneMaxProblem;
import dk.dtu.scout.searchSpace.BitString;
import dk.dtu.scout.stopcondition.MaxIterations;
import org.springframework.stereotype.Service;

@Service
public class CatalogService {

    private static ParamDef toParamDef(Parameter param) {
        return new ParamDef(param.key(), param.label(), param.type(), param.defaultValue(), param.min(), param.max());
    }

    private static List<ParamDef> toParamDefs(List<Parameter> params) {
        return params.stream().map(CatalogService::toParamDef).toList();
    }

    private static SearchSpaceDef toSearchSpaceDef(Component component) {
        return new SearchSpaceDef(component.id(), component.displayName(), component.description(), toParamDefs(component.params()));
    }

    private static ProblemDef toProblemDef(Component component) {
        return new ProblemDef(component.id(), component.displayName(), component.description(), toParamDefs(component.params()));
    }

    private static AlgoDef toAlgoDef(Component component) {
        return new AlgoDef(component.id(), component.displayName(), component.description(), toParamDefs(component.params()));
    }

    private static MutationDef toMutationDef(Component component) {
        return new MutationDef(component.id(), component.displayName(), component.description(), toParamDefs(component.params()));
    }

    private static AcceptanceRuleDef toAcceptanceRuleDef(Component component) {
        return new AcceptanceRuleDef(component.id(), component.displayName(), component.description(), toParamDefs(component.params()));
    }

    private static PopulationModelDef toPopulationModelDef(Component component) {
        return new PopulationModelDef(component.id(), component.displayName(), component.description(), toParamDefs(component.params()));
    }

    private static StopConditionDef toStopConditionDef(Component component) {
        return new StopConditionDef(component.id(), component.displayName(), component.description(), toParamDefs(component.params()));
    }

    private static ObserverDef toObserverDef(Component component) {
        return new ObserverDef(component.id(), component.displayName(), component.description(), toParamDefs(component.params()));
    }

    /** Returns the list of available search spaces.
     * @return List of SearchSpaceDef
     */
    public List<SearchSpaceDef> searchSpaces() {
        return List.of(
            toSearchSpaceDef(new BitString(100))
        );
    }

    /** Returns the list of available problems.
     * @return List of ProblemDef
     */
    public List<ProblemDef> problems() {
        return List.of(
            toProblemDef(new OneMaxProblem(100)),
            toProblemDef(new LeadingOnesProblem(100))
        );
    }

    /** Returns the list of available algorithms.
     * @return List of AlgoDef
     */
    public List<AlgoDef> algorithms() {
        return List.of(
            toAlgoDef(new OnePlusOneEA<>(null, null)),
            toAlgoDef(new SimulatedAnnealing<>(null, null))
        );
    }

    /**
     * Returns the list of available mutations.
     * @return List of MutationDef
     */
    public List<MutationDef> mutations() {
        return List.of(
            toMutationDef(BitMutation.withProbability(0.01)),
            toMutationDef(BitMutation.singleBit())
        );
    }

    /**
     * Returns the list of available acceptance rules.
     * @return List of AcceptanceRuleDef
     */
    public List<AcceptanceRuleDef> acceptanceRules() {
        return List.of(
            toAcceptanceRuleDef(new ElitistAcceptance()),
            toAcceptanceRuleDef(new SimulatedAnnealingAcceptance(5.0, 0.995, 1e-6))
        );
    }

    /**
     * Returns the list of available population models.
     * @return List of PopulationModelDef
     */
    public List<PopulationModelDef> populationModels() {
        return List.of(
            toPopulationModelDef(new DefaultPopulationModel<>())
        );
    }

    /**
     * Returns the list of available stop conditions.
     * @return List of StopConditionDef
     */
    public List<StopConditionDef> stopConditions() {
        return List.of(
            toStopConditionDef(new MaxIterations<>(10_000))
        );
    }

    /**
     * Returns the list of available observers.
     * @return List of ObserverDef
     */
    public List<ObserverDef> observers() {
        return List.of(
            toObserverDef(new FitnessObserver<>()),
            toObserverDef(new AcceptanceRateObserver<>()),
            toObserverDef(new ImprovementObserver<>())
        );
    }
}