package dk.dtu.scout.observer;

import dk.dtu.scout.Component;
import dk.dtu.scout.logging.RunLog;
import dk.dtu.scout.logging.RunState;

public interface Observer<S> extends Component  {
    String id();
    String displayName();

    default void onStart(RunState<S> state, RunLog log) {}
    void onStep(RunState<S> state, RunLog log);
    default void onEnd(RunState<S> state, RunLog log) {}
}
