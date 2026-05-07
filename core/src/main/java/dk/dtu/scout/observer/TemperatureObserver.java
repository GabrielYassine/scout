package dk.dtu.scout.observer;

import dk.dtu.scout.State;
import dk.dtu.scout.datatypes.StateKeys;
import dk.dtu.scout.dto.Parameter;
import dk.dtu.scout.logging.IterationSnapshot;
import dk.dtu.scout.logging.RunLog;
import dk.dtu.scout.logging.SeriesMode;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Tracks the current temperature.
 * @author s235257
 */
@Component
@Scope("prototype")
public class TemperatureObserver<S> implements Observer<S> {

    private State state;

    @Override
    public String id() {
        return "temperature";
    }

    @Override
    public String displayName() {
        return "Temperature";
    }

    @Override
    public String description() {
        return "Tracks the current temperature used by annealed selection";
    }

    @Override
    public List<Parameter> params() {
        return List.of();
    }

    @Override
    public void init(State state) {
        this.state = state;
    }

    @Override
    public void onStep(IterationSnapshot<S> snapshot, RunLog log) {
        Object temperatureObj = state.get(StateKeys.TEMPERATURE);
        if (temperatureObj instanceof Number temperature) {
            log.putSeries("temperature", temperature.doubleValue(), SeriesMode.ALL);
        }
    }
}