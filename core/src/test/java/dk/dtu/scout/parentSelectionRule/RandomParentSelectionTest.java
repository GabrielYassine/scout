package dk.dtu.scout.parentSelectionRule;

import dk.dtu.scout.dto.EvaluatedSolution;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class RandomParentSelectionTest {

    @Test
    void selectParents_returnsTwoParentsFromPopulation() {
        RandomParentSelection<String> selection = new RandomParentSelection<>();

        List<EvaluatedSolution<String>> parents = List.of(
            evaluated("a", 1.0),
            evaluated("b", 2.0),
            evaluated("c", 3.0)
        );

        List<EvaluatedSolution<String>> selected = selection.selectParents(parents, new Random(1234L));

        assertEquals(2, selected.size());
        assertTrue(parents.contains(selected.get(0)));
        assertTrue(parents.contains(selected.get(1)));
    }

    @Test
    void selectParents_canSelectSameParentTwice() {
        RandomParentSelection<String> selection = new RandomParentSelection<>();
        EvaluatedSolution<String> onlyParent = evaluated("only", 1.0);

        List<EvaluatedSolution<String>> selected = selection.selectParents(List.of(onlyParent), new Random(1234L));

        assertEquals(2, selected.size());
        assertSame(onlyParent, selected.get(0));
        assertSame(onlyParent, selected.get(1));
    }

    @Test
    void selectParents_rejectsNullParents() {
        RandomParentSelection<String> selection = new RandomParentSelection<>();
        assertThrows(IllegalStateException.class, () -> selection.selectParents(null, new Random(1234L)));
    }

    @Test
    void selectParents_rejectsEmptyParents() {
        RandomParentSelection<String> selection = new RandomParentSelection<>();
        assertThrows(IllegalStateException.class, () -> selection.selectParents(List.of(), new Random(1234L)));
    }

    @Test
    void metadata_isStable() {
        RandomParentSelection<String> selection = new RandomParentSelection<>();

        assertEquals("random-parents", selection.id());
        assertEquals("Random Parent Selection", selection.displayName());
        assertFalse(selection.description().isBlank());
        assertTrue(selection.params().isEmpty());
    }

    private static EvaluatedSolution<String> evaluated(String value, double fitness) {
        return new EvaluatedSolution<>(value, fitness);
    }
}