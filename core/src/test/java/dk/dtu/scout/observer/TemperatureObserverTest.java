package dk.dtu.scout.observer;

import dk.dtu.scout.State;
import dk.dtu.scout.datatypes.StateKeys;
import dk.dtu.scout.logging.IterationSnapshot;
import dk.dtu.scout.logging.RunLog;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TemperatureObserverTest {

    @Test
    void onStep_doesNothingWhenStateWasNotInitialized() {
        TemperatureObserver<Object> observer = new TemperatureObserver<>();
        RunLog log = new RunLog();

        observer.onStep(new IterationSnapshot<>(0, 0, null, null, false), log);

        assertTrue(log.getSeries().isEmpty());
    }

    @Test
    void onStep_logsTemperatureWhenStateContainsNumber() {
        TemperatureObserver<Object> observer = new TemperatureObserver<>();
        State state = new State();
        RunLog log = new RunLog();

        state.update(Map.of(StateKeys.TEMPERATURE, 2.5));
        observer.init(state);

        observer.onStep(new IterationSnapshot<>(0, 0, null, null, false), log);

        assertEquals(List.of(2.5), log.getSeries().get("temperature").getValues());
    }

    @Test
    void onStep_ignoresNonNumericTemperature() {
        TemperatureObserver<Object> observer = new TemperatureObserver<>();
        State state = new State();
        RunLog log = new RunLog();

        state.update(Map.of(StateKeys.TEMPERATURE, "hot"));
        observer.init(state);

        observer.onStep(new IterationSnapshot<>(0, 0, null, null, false), log);

        assertTrue(log.getSeries().isEmpty());
    }

    @Test
    void metadata_isStable() {
        TemperatureObserver<Object> observer = new TemperatureObserver<>();

        assertEquals("temperature", observer.id());
        assertEquals("Temperature", observer.displayName());
        assertFalse(observer.description().isBlank());
        assertTrue(observer.params().isEmpty());
    }
}