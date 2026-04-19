package dk.dtu.scout.observer;

import dk.dtu.scout.dto.Parameter;
import dk.dtu.scout.logging.RunLog;
import dk.dtu.scout.logging.SeriesMode;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Tracks fitness values over time.
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


    @Override
    public void onStep(IterationSnapshot<S> state, RunLog log) {
        log.putSeries("fitness", state.currentFitness(), SeriesMode.ALL);
        log.putSeries("bestFitness", state.bestFitness(), SeriesMode.ALL);
    }
}
