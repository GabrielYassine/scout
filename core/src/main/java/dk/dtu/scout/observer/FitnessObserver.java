package dk.dtu.scout.observer;

import dk.dtu.scout.dto.Parameter;
import dk.dtu.scout.logging.RunLog;
import dk.dtu.scout.logging.IterationSnapshot;
import dk.dtu.scout.logging.SeriesMode;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Observer that records the current fitness and best fitness during a run.
 * The current fitness represents the fitness of the current representative
 * solution, while bestFitness represents the best solution found so far.
 * @author s235257 & s230632
 */

@Component
@Scope("prototype")
public class FitnessObserver<S> implements Observer<S> {

    @Override
    public String id() {
        return "fitness";
    }

    @Override
    public String displayName() {
        return "Fitness Tracker";
    }

    @Override
    public String description() {
        return "Tracks fitness values over time";
    }

    @Override
    public List<Parameter> params() {
        return List.of();
    }
    /**
     * Logs the current fitness and best fitness for the current evaluation point.
     * @param state current iteration snapshot
     * @param log run log where the fitness values are stored
     */
    @Override
    public void onStep(IterationSnapshot<S> state, RunLog log) {
        log.putSeries("fitness", state.currentFitness(), SeriesMode.ALL);
        log.putSeries("bestFitness", state.bestFitness(), SeriesMode.ALL);
    }
}
