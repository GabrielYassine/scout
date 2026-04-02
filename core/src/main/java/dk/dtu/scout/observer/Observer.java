package dk.dtu.scout.observer;

import dk.dtu.scout.ScoutComponent;
import dk.dtu.scout.logging.RunLog;
import dk.dtu.scout.logging.RunState;

/**
 * Observer hook for monitoring runs and emitting metrics into {@link RunLog}.
 */
public interface Observer<S> extends ScoutComponent {
    default void onStart(RunState<S> state, RunLog log) {}
    void onStep(RunState<S> state, RunLog log);
    default void onEnd(RunState<S> state, RunLog log) {}
}
