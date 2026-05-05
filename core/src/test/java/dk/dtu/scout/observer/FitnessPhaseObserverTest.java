package dk.dtu.scout.observer;

import dk.dtu.scout.dto.EvaluatedSolution;
import dk.dtu.scout.logging.IterationSnapshot;
import dk.dtu.scout.logging.RunLog;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FitnessPhaseObserverTest {

    @Test
    void onStep_emitsImprovingIntervalWhenBlockIsFull() {
        FitnessPhaseObserver<Object> observer = new FitnessPhaseObserver<>();
        RunLog log = new RunLog();

        observer.configure(Map.of("windowSize", 3, "epsilon", 0.1));
        observer.onStart(snapshot(0, 1.0), log);

        observer.onStep(snapshot(1, 1.0), log);
        observer.onStep(snapshot(2, 1.2), log);
        observer.onStep(snapshot(3, 1.5), log);

        List<?> intervals = log.getSeries().get("fitnessPhaseIntervals").getValues();
        assertEquals(1, intervals.size());

        Map<?, ?> interval = (Map<?, ?>) intervals.getFirst();
        assertEquals(0, interval.get("startEvaluation"));
        assertEquals(2, interval.get("endEvaluation"));
        assertEquals("IMPROVING", interval.get("phase"));
    }

    @Test
    void onStep_emitsWorseningIntervalWhenFitnessDropsBeyondEpsilon() {
        FitnessPhaseObserver<Object> observer = new FitnessPhaseObserver<>();
        RunLog log = new RunLog();

        observer.configure(Map.of("windowSize", 2, "epsilon", 0.1));
        observer.onStart(snapshot(0, 5.0), log);

        observer.onStep(snapshot(1, 5.0), log);
        observer.onStep(snapshot(2, 4.0), log);

        Map<?, ?> interval = firstInterval(log);

        assertEquals("WORSENING", interval.get("phase"));
    }

    @Test
    void onStep_emitsStagnantIntervalWhenChangeIsWithinEpsilon() {
        FitnessPhaseObserver<Object> observer = new FitnessPhaseObserver<>();
        RunLog log = new RunLog();

        observer.configure(Map.of("windowSize", 2, "epsilon", 0.5));
        observer.onStart(snapshot(0, 5.0), log);

        observer.onStep(snapshot(1, 5.0), log);
        observer.onStep(snapshot(2, 5.2), log);

        Map<?, ?> interval = firstInterval(log);

        assertEquals("STAGNANT", interval.get("phase"));
    }

    @Test
    void onStep_waitsUntilWindowIsFull() {
        FitnessPhaseObserver<Object> observer = new FitnessPhaseObserver<>();
        RunLog log = new RunLog();

        observer.configure(Map.of("windowSize", 3));
        observer.onStart(snapshot(0, 1.0), log);

        observer.onStep(snapshot(1, 1.0), log);
        observer.onStep(snapshot(2, 2.0), log);

        assertFalse(log.getSeries().containsKey("fitnessPhaseIntervals"));
    }

    @Test
    void onEnd_emitsPartialBlock() {
        FitnessPhaseObserver<Object> observer = new FitnessPhaseObserver<>();
        RunLog log = new RunLog();

        observer.configure(Map.of("windowSize", 5, "epsilon", 0.1));
        observer.onStart(snapshot(0, 1.0), log);

        observer.onStep(snapshot(1, 1.0), log);
        observer.onStep(snapshot(2, 2.0), log);

        observer.onEnd(snapshot(2, 2.0), log);

        Map<?, ?> interval = firstInterval(log);

        assertEquals(0, interval.get("startEvaluation"));
        assertEquals(1, interval.get("endEvaluation"));
        assertEquals("IMPROVING", interval.get("phase"));
    }

    @Test
    void onEnd_doesNothingWhenThereIsNoPartialBlock() {
        FitnessPhaseObserver<Object> observer = new FitnessPhaseObserver<>();
        RunLog log = new RunLog();

        observer.onStart(snapshot(0, 1.0), log);
        observer.onEnd(snapshot(0, 1.0), log);

        assertFalse(log.getSeries().containsKey("fitnessPhaseIntervals"));
    }

    @Test
    void intervalsContinueFromPreviousIntervalEnd() {
        FitnessPhaseObserver<Object> observer = new FitnessPhaseObserver<>();
        RunLog log = new RunLog();

        observer.configure(Map.of("windowSize", 2, "epsilon", 0.0));
        observer.onStart(snapshot(0, 1.0), log);

        observer.onStep(snapshot(1, 1.0), log);
        observer.onStep(snapshot(2, 2.0), log);

        observer.onStep(snapshot(3, 2.0), log);
        observer.onStep(snapshot(4, 1.0), log);

        List<?> intervals = log.getSeries().get("fitnessPhaseIntervals").getValues();

        Map<?, ?> first = (Map<?, ?>) intervals.get(0);
        Map<?, ?> second = (Map<?, ?>) intervals.get(1);

        assertEquals(0, first.get("startEvaluation"));
        assertEquals(1, first.get("endEvaluation"));

        assertEquals(1, second.get("startEvaluation"));
        assertEquals(3, second.get("endEvaluation"));
        assertEquals("WORSENING", second.get("phase"));
    }

    @Test
    void onStart_resetsBufferedValuesAndIntervalState() {
        FitnessPhaseObserver<Object> observer = new FitnessPhaseObserver<>();
        RunLog log = new RunLog();

        observer.configure(Map.of("windowSize", 2, "epsilon", 0.0));

        observer.onStart(snapshot(0, 1.0), log);
        observer.onStep(snapshot(1, 1.0), log);
        observer.onStep(snapshot(2, 2.0), log);

        observer.onStart(snapshot(10, 10.0), log);
        observer.onStep(snapshot(11, 10.0), log);
        observer.onStep(snapshot(12, 11.0), log);

        List<?> intervals = log.getSeries().get("fitnessPhaseIntervals").getValues();
        Map<?, ?> second = (Map<?, ?>) intervals.get(1);

        assertEquals(10, second.get("startEvaluation"));
        assertEquals(11, second.get("endEvaluation"));
    }

    @Test
    void configure_rejectsInvalidValues() {
        FitnessPhaseObserver<Object> observer = new FitnessPhaseObserver<>();

        assertThrows(IllegalArgumentException.class, () ->
            observer.configure(Map.of("windowSize", 0))
        );

        assertThrows(IllegalArgumentException.class, () ->
            observer.configure(Map.of("windowSize", -1))
        );

        assertThrows(IllegalArgumentException.class, () ->
            observer.configure(Map.of("epsilon", -0.1))
        );
    }

    @Test
    void configure_ignoresNullParams() {
        FitnessPhaseObserver<Object> observer = new FitnessPhaseObserver<>();

        observer.configure(null);

        assertEquals(2, observer.params().size());
    }

    @Test
    void metadata_isStable() {
        FitnessPhaseObserver<Object> observer = new FitnessPhaseObserver<>();

        assertEquals("fitness-phase", observer.id());
        assertEquals("Fitness Phase Observer", observer.displayName());
        assertFalse(observer.description().isBlank());
        assertEquals(2, observer.params().size());
    }

    private static IterationSnapshot<Object> snapshot(int evaluations, double fitness) {
        EvaluatedSolution<Object> solution = new EvaluatedSolution<>(new Object(), fitness);
        return new IterationSnapshot<>(evaluations, evaluations, solution, solution, true);
    }

    private static Map<?, ?> firstInterval(RunLog log) {
        return (Map<?, ?>) log.getSeries()
            .get("fitnessPhaseIntervals")
            .getValues()
            .getFirst();
    }
}