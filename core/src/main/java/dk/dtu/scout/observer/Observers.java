package dk.dtu.scout.observer;

import dk.dtu.scout.logging.RunLog;
import dk.dtu.scout.logging.RunState;

import java.util.List;

public final class Observers {

    private Observers() {}

    public static <S> void onStart(List<Observer<S>> observers, RunState<S> state, RunLog log) {
        if (observers == null) return;
        for (Observer<S> observer : observers) {
            observer.onStart(state, log);
        }
    }

    public static <S> void onStep(List<Observer<S>> observers, RunState<S> state, RunLog log) {
        if (observers == null) return;
        for (Observer<S> observer : observers) {
            observer.onStep(state, log);
        }
    }

    public static <S> void onEnd(List<Observer<S>> observers, RunState<S> state, RunLog log) {
        if (observers == null) return;
        for (Observer<S> observer : observers) {
            observer.onEnd(state, log);
        }
    }
}