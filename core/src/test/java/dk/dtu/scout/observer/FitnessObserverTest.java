package dk.dtu.scout.observer;

import dk.dtu.scout.dto.EvaluatedSolution;
import dk.dtu.scout.logging.IterationSnapshot;
import dk.dtu.scout.logging.RunLog;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FitnessObserverTest {

    @Test
    void onStep_logsCurrentAndBestFitness() {
        FitnessObserver<String> observer = new FitnessObserver<>();
        RunLog log = new RunLog();

        IterationSnapshot<String> snapshot = new IterationSnapshot<>(
            1,
            2,
            new EvaluatedSolution<>("current", 3.0),
            new EvaluatedSolution<>("best", 5.0),
            true
        );

        observer.onStep(snapshot, log);

        assertEquals(List.of(3.0), log.getSeries().get("fitness").getValues());
        assertEquals(List.of(5.0), log.getSeries().get("bestFitness").getValues());
    }

    @Test
    void metadata_isStable() {
        FitnessObserver<Object> observer = new FitnessObserver<>();

        assertEquals("fitness", observer.id());
        assertEquals("Fitness Tracker", observer.displayName());
        assertFalse(observer.description().isBlank());
        assertTrue(observer.params().isEmpty());
    }
}