package dk.dtu.scout.algorithm;

import dk.dtu.scout.acceptance.AcceptanceRule;
import dk.dtu.scout.logging.RunLog;
import dk.dtu.scout.mutation.Mutation;
import dk.dtu.scout.observer.Observer;
import dk.dtu.scout.population.PopulationModel;
import dk.dtu.scout.problems.Problem;
import dk.dtu.scout.searchSpace.SearchSpace;
import dk.dtu.scout.stopcondition.StopCondition;

import java.util.Random;

public class VariationAlgorithm<S> implements Algorithm<S> {

    private final Mutation<S> mutation;
    private final AcceptanceRule acceptance;
    private final PopulationModel<S> populationModel;

    public VariationAlgorithm(
            Mutation<S> mutation,
            AcceptanceRule acceptance,
            PopulationModel<S> populationModel
    ) {
        this.mutation = mutation;
        this.acceptance = acceptance;
        this.populationModel = populationModel;
    }

    @Override
    public RunLog run(
            SearchSpace<S> space,
            Problem<S> problem,
            Random rng,
            StopCondition<S> stop,
            Observer<S> observer
    ) {
        return populationModel.run(mutation, acceptance, space, problem, rng, stop, observer);
    }

    public Mutation<S> getMutation() {
        return mutation;
    }

    public AcceptanceRule getAcceptance() {
        return acceptance;
    }

    public PopulationModel<S> getPopulationModel() {
        return populationModel;
    }
}
