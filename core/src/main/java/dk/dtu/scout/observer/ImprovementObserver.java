package dk.dtu.scout.observer;

import dk.dtu.scout.Parameter;
import dk.dtu.scout.logging.RunLog;
import dk.dtu.scout.logging.RunState;

import java.util.List;

/**
 * Tracks improvements over time.
 */
public class ImprovementObserver<S> implements Observer<S> {
    private double previousBestFitness = Double.NEGATIVE_INFINITY;

    @Override
    public String id() {
        return "improvements";
    }

    @Override
    public String displayName() {
        return "Improvement Tracker";
    }

    @Override
    public String description() {
        return "Tracks improvements in the best solution";
    }

    @Override
    public List<Parameter> params() {
        return List.of();
    }

    @Override
    public void onStart(RunState<S> state, RunLog log) {
        previousBestFitness = state.bestFitness();
    }

    @Override
    public void onStep(RunState<S> state, RunLog log) {
        log.tick(state.iteration());

        double improvement = 0.0;
        if (state.bestFitness() > previousBestFitness) {
            improvement = state.bestFitness() - previousBestFitness;
            previousBestFitness = state.bestFitness();
        }

        log.put("improvement", improvement);
    }
}
