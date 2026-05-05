package dk.dtu.scout.parentSelectionRule;

import dk.dtu.scout.dto.EvaluatedSolution;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class ElitistParentSelectionTest {

    @Test
    void selectParents_returnsTwoBestParentsByFitness() {
        ElitistParentSelection<String> selection = new ElitistParentSelection<>();

        List<EvaluatedSolution<String>> selected = selection.selectParents(
            List.of(
                evaluated("low", 1.0),
                evaluated("best", 10.0),
                evaluated("second", 5.0)
            ),
            new Random(1234L)
        );

        assertEquals(2, selected.size());
        assertEquals("best", selected.get(0).value());
        assertEquals("second", selected.get(1).value());
    }

    @Test
    void selectParents_duplicatesOnlyParentWhenPopulationHasOneElement() {
        ElitistParentSelection<String> selection = new ElitistParentSelection<>();
        EvaluatedSolution<String> parent = evaluated("only", 1.0);

        List<EvaluatedSolution<String>> selected = selection.selectParents(List.of(parent), new Random(1234L));

        assertEquals(2, selected.size());
        assertSame(parent, selected.get(0));
        assertSame(parent, selected.get(1));
    }

    @Test
    void selectParents_rejectsNullParents() {
        ElitistParentSelection<String> selection = new ElitistParentSelection<>();
        assertThrows(IllegalStateException.class, () -> selection.selectParents(null, new Random(1234L)));
    }

    @Test
    void selectParents_rejectsEmptyParents() {
        ElitistParentSelection<String> selection = new ElitistParentSelection<>();
        assertThrows(IllegalStateException.class, () -> selection.selectParents(List.of(), new Random(1234L)));
    }

    @Test
    void metadata_isStable() {
        ElitistParentSelection<String> selection = new ElitistParentSelection<>();

        assertEquals("elitist-parents", selection.id());
        assertEquals("Elitist Parent Selection", selection.displayName());
        assertFalse(selection.description().isBlank());
        assertTrue(selection.params().isEmpty());
    }

    private static EvaluatedSolution<String> evaluated(String value, double fitness) {
        return new EvaluatedSolution<>(value, fitness);
    }
}