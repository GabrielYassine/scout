package dk.dtu.scout.observer;

import dk.dtu.scout.dto.EvaluatedSolution;
import dk.dtu.scout.logging.IterationSnapshot;
import dk.dtu.scout.logging.RunLog;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HypercubeObserverTest {

    @Test
    void onStep_mapsEmptyBitstringToOrigin() {
        HypercubeObserver observer = new HypercubeObserver();
        RunLog log = new RunLog();

        observer.onStep(snapshot(new boolean[0]), log);

        assertEquals(List.of(0.0), log.getSeries().get("hypercubeX").getValues());
        assertEquals(List.of(0.0), log.getSeries().get("hypercubeY").getValues());
    }

    @Test
    void onStep_mapsAllFalseBitstringToBottomCenter() {
        HypercubeObserver observer = new HypercubeObserver();
        RunLog log = new RunLog();

        observer.onStep(snapshot(new boolean[] {false, false, false, false}), log);

        double x = (double) log.getSeries().get("hypercubeX").getValues().getFirst();
        double y = (double) log.getSeries().get("hypercubeY").getValues().getFirst();

        assertEquals(0.0, x, 1e-9);
        assertEquals(0.0, y, 1e-9);
    }

    @Test
    void onStep_mapsAllTrueBitstringToTopCenter() {
        HypercubeObserver observer = new HypercubeObserver();
        RunLog log = new RunLog();

        observer.onStep(snapshot(new boolean[] {true, true, true, true}), log);

        double x = (double) log.getSeries().get("hypercubeX").getValues().getFirst();
        double y = (double) log.getSeries().get("hypercubeY").getValues().getFirst();

        assertEquals(0.0, x, 1e-9);
        assertEquals(1.0, y, 1e-9);
    }

    @Test
    void onStep_mapsMixedBitstringToExpectedProjection() {
        HypercubeObserver observer = new HypercubeObserver();
        RunLog log = new RunLog();

        observer.onStep(snapshot(new boolean[] {true, false, true, false}), log);

        double x = (double) log.getSeries().get("hypercubeX").getValues().getFirst();
        double y = (double) log.getSeries().get("hypercubeY").getValues().getFirst();

        assertEquals(-0.5, x, 1e-9);
        assertEquals(0.5, y, 1e-9);
    }

    @Test
    void onStep_appendsValuesAcrossMultipleSteps() {
        HypercubeObserver observer = new HypercubeObserver();
        RunLog log = new RunLog();

        observer.onStep(snapshot(new boolean[] {false, false}), log);
        observer.onStep(snapshot(new boolean[] {true, true}), log);

        assertEquals(List.of(0.0, 0.0), log.getSeries().get("hypercubeX").getValues());
        assertEquals(List.of(0.0, 1.0), log.getSeries().get("hypercubeY").getValues());
    }

    @Test
    void metadata_isStable() {
        HypercubeObserver observer = new HypercubeObserver();

        assertEquals("hypercube", observer.id());
        assertEquals("Hypercube (2D Projection)", observer.displayName());
        assertFalse(observer.description().isBlank());
        assertTrue(observer.params().isEmpty());
        assertEquals(List.of("bitstring"), observer.supportedSearchSpaces());
    }

    private static IterationSnapshot<boolean[]> snapshot(boolean[] solution) {
        EvaluatedSolution<boolean[]> evaluated = new EvaluatedSolution<>(solution, 0.0);
        return new IterationSnapshot<>(0, 0, evaluated, evaluated, true);
    }
}