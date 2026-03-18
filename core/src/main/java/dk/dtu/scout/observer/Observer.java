package dk.dtu.scout.observer;

import dk.dtu.scout.ScoutComponent;
import dk.dtu.scout.logging.RunLog;
import dk.dtu.scout.logging.RunState;

import java.util.Map;

public interface Observer<S> extends ScoutComponent {
    String id();
    String displayName();

    default void onStart(RunState<S> state, RunLog log) {}
    void onStep(RunState<S> state, RunLog log);
    default void onEnd(RunState<S> state, RunLog log) {}
    default void configure(Map<String, Object> params) {}
}
