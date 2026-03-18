package dk.dtu.scout.algorithm;

import dk.dtu.scout.acceptance.AcceptanceRule;
import dk.dtu.scout.logging.RunLog;
import dk.dtu.scout.mutation.Generator;
import dk.dtu.scout.observer.Observer;
import dk.dtu.scout.population.PopulationModel;
import dk.dtu.scout.problems.Problem;
import dk.dtu.scout.searchSpace.SearchSpace;
import dk.dtu.scout.stopcondition.StopCondition;

import java.util.Random;

public class VariationAlgorithm<S> implements Algorithm<S> {

    private final Generator<S> generator;
    private final AcceptanceRule acceptance;
    private final PopulationModel<S> populationModel;

    public VariationAlgorithm(
            Generator<S> generator,
            AcceptanceRule acceptance,
            PopulationModel<S> populationModel
    ) {
        this.generator = generator;
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
        return populationModel.run(generator, acceptance, space, problem, rng, stop, observer);
    }

    public Generator<S> getMutation() {
        return generator;
    }

    public AcceptanceRule getAcceptance() {
        return acceptance;
    }

    public PopulationModel<S> getPopulationModel() {
        return populationModel;
    }
}
