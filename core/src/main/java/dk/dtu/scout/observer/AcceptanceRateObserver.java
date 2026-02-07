package dk.dtu.scout.observer;

import dk.dtu.scout.logging.RunLog;
import dk.dtu.scout.logging.RunState;

/**
 * Tracks acceptance rate over a sliding window.
 */
public class AcceptanceRateObserver<S> implements Observer<S> {
    private int acceptedCount = 0;
    private int totalCount = 0;

    @Override
    public String id() {
        return "acceptance-rate";
    }

    @Override
    public String displayName() {
        return "Acceptance Rate";
    }

    @Override
    public void onStep(RunState<S> state, RunLog log) {
        totalCount++;
        if (state.accepted()) {
            acceptedCount++;
        }

        log.tick(state.iteration());
        double rate = totalCount > 0 ? (double) acceptedCount / totalCount : 0.0;
        log.put("acceptanceRate", rate);
    }
}
