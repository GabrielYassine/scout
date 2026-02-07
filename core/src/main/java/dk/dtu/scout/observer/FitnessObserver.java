package dk.dtu.scout.observer;

import dk.dtu.scout.logging.RunLog;
import dk.dtu.scout.logging.RunState;

/**
 * Tracks fitness values over time.
 */
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
    public void onStep(RunState<S> state, RunLog log) {
        log.tick(state.iteration());
        log.put("fitness", state.currentFitness());
        log.put("bestFitness", state.bestFitness());
    }
}
