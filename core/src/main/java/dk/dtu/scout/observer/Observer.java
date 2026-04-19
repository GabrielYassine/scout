package dk.dtu.scout.observer;

import dk.dtu.scout.ScoutComponent;
import dk.dtu.scout.logging.IterationSnapshot;
import dk.dtu.scout.logging.RunLog;

/**
 * Observer hook for monitoring runs and emitting metrics into {@link RunLog}.
 */
public interface Observer<S> extends ScoutComponent {
    default void onStart(IterationSnapshot<S> state, RunLog log) {}
    void onStep(IterationSnapshot<S> state, RunLog log);
    default void onEnd(IterationSnapshot<S> state, RunLog log) {}
}
