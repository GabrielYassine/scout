package dk.dtu.scout.observer;

import dk.dtu.scout.Parameter;
import dk.dtu.scout.logging.RunLog;
import dk.dtu.scout.logging.RunState;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Tracks acceptance rate over a sliding window.
 */

@Component
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
    public String description() {
        return "Tracks the acceptance rate of solutions";
    }

    @Override
    public List<Parameter> params() {
        return List.of();
    }

    @Override
    public void onStep(RunState<S> state, RunLog log) {
        totalCount++;
        if (state.accepted()) {
            acceptedCount++;
        }
        double rate = totalCount > 0 ? (double) acceptedCount / totalCount : 0.0;
        log.put("acceptanceRate", rate);
    }
}
