package dk.dtu.scout.observer;

import dk.dtu.scout.Parameter;
import dk.dtu.scout.logging.RunLog;
import dk.dtu.scout.logging.RunState;

import java.util.List;

public class BroadcastObserver<S> implements Observer<S> {
    private final List<Observer<S>> observers;

    public BroadcastObserver(List<Observer<S>> observers) {
        this.observers = observers;
    }

    @Override
    public String id() {
        return "";
    }

    @Override
    public String displayName() {
        return "";
    }

    @Override
    public String description() {
        return "";
    }

    @Override
    public List<Parameter> params() {
        return List.of();
    }

    @Override
    public void onStart(RunState<S> state, RunLog log) {
        for (var o : observers) o.onStart(state, log);
    }
    @Override
    public void onStep(RunState<S> state, RunLog log) {
        for (var o : observers) o.onStep(state, log);
    }
    @Override
    public void onEnd(RunState<S> state, RunLog log) {
        for (var o : observers) o.onEnd(state, log);
    }
}