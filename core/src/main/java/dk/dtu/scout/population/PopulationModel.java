package dk.dtu.scout.population;

import dk.dtu.scout.Component;
import dk.dtu.scout.acceptance.AcceptanceRule;
import dk.dtu.scout.logging.RunLog;
import dk.dtu.scout.mutation.Mutation;
import dk.dtu.scout.observer.Observer;
import dk.dtu.scout.problems.Problem;
import dk.dtu.scout.searchSpace.SearchSpace;
import dk.dtu.scout.stopcondition.StopCondition;

import java.util.Map;
import java.util.Random;

public interface PopulationModel<S> extends Component  {

    RunLog run(Mutation<S> mutation, AcceptanceRule acceptance, SearchSpace<S> space, Problem<S> problem, Random rng, StopCondition<S> stop, Observer<S> observer
    );
    default void configure(Map<String, Object> params) {}
}
