package dk.dtu.scout.logging;

import dk.dtu.scout.dto.EvaluatedSolution;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IterationSnapshotTest {

    @Test
    void accessorsReturnValuesFromCurrentAndBestSolutions() {
        EvaluatedSolution<String> current = new EvaluatedSolution<>("current", 1.5);
        EvaluatedSolution<String> best = new EvaluatedSolution<>("best", 2.5);

        IterationSnapshot<String> snapshot = new IterationSnapshot<>(
            3,
            10,
            current,
            best,
            true
        );

        assertEquals("current", snapshot.currentSolution());
        assertEquals(1.5, snapshot.currentFitness());
        assertEquals("best", snapshot.bestSolution());
        assertEquals(2.5, snapshot.bestFitness());
        assertTrue(snapshot.accepted());
    }

    @Test
    void accessorsHandleNullCurrentAndBest() {
        IterationSnapshot<String> snapshot = new IterationSnapshot<>(
            0,
            0,
            null,
            null,
            false
        );

        assertNull(snapshot.currentSolution());
        assertTrue(Double.isNaN(snapshot.currentFitness()));
        assertNull(snapshot.bestSolution());
        assertTrue(Double.isNaN(snapshot.bestFitness()));
        assertFalse(snapshot.accepted());
    }
}