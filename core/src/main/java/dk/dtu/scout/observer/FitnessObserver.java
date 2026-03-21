package dk.dtu.scout.observer;

import dk.dtu.scout.Parameter;
import dk.dtu.scout.logging.RunLog;
import dk.dtu.scout.logging.RunState;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Tracks fitness values over time.
 */

@Component
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
    public List<String> supportedSearchSpaces() {
        return List.of("bitstring");
    }

    @Override
    public void onStep(RunState<S> state, RunLog log) {
        log.put("fitness", state.currentFitness());
        log.put("bestFitness", state.bestFitness());
    }
}
