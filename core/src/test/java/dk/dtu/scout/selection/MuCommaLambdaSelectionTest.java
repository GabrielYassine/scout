package dk.dtu.scout.selection;

import dk.dtu.scout.dto.EvaluatedSolution;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class MuCommaLambdaSelectionTest {

    @Test
    void select_returnsBestMuChildrenOnly() {
        MuCommaLambdaSelection<String> selection = new MuCommaLambdaSelection<>();

        List<EvaluatedSolution<String>> parents = List.of(evaluated("parent-best", 100.0));

        List<EvaluatedSolution<String>> children = List.of(
            evaluated("child-low", 1.0),
            evaluated("child-best", 10.0),
            evaluated("child-mid", 5.0)
        );

        List<EvaluatedSolution<String>> selected = selection.select(
            parents,
            children,
            2,
            0,
            new Random(1234L)
        );

        assertEquals(2, selected.size());
        assertEquals("child-best", selected.get(0).value());
        assertEquals("child-mid", selected.get(1).value());
    }

    @Test
    void select_rejectsNullChildren() {
        MuCommaLambdaSelection<String> selection = new MuCommaLambdaSelection<>();
        assertThrows(IllegalStateException.class, () -> selection.select(List.of(evaluated("parent", 1.0)), null, 1, 0, new Random(1234L)));
    }

    @Test
    void select_rejectsEmptyChildren() {
        MuCommaLambdaSelection<String> selection = new MuCommaLambdaSelection<>();
        assertThrows(IllegalStateException.class, () -> selection.select(List.of(evaluated("parent", 1.0)), List.of(), 1, 0, new Random(1234L)));
    }

    @Test
    void select_rejectsNonPositiveMu() {
        MuCommaLambdaSelection<String> selection = new MuCommaLambdaSelection<>();

        List<EvaluatedSolution<String>> children = List.of(evaluated("child", 1.0));

        assertThrows(IllegalArgumentException.class, () -> selection.select(List.of(), children, 0, 0, new Random(1234L)));

        assertThrows(IllegalArgumentException.class, () -> selection.select(List.of(), children, -1, 0, new Random(1234L)));
    }

    @Test
    void select_rejectsMuLargerThanChildCount() {
        MuCommaLambdaSelection<String> selection = new MuCommaLambdaSelection<>();

        List<EvaluatedSolution<String>> children = List.of(evaluated("child", 1.0));

        assertThrows(IllegalArgumentException.class, () -> selection.select(List.of(), children, 2, 0, new Random(1234L)));
    }

    @Test
    void metadata_isStable() {
        MuCommaLambdaSelection<String> selection = new MuCommaLambdaSelection<>();

        assertEquals("mu-comma-lambda", selection.id());
        assertEquals("(mu,lambda) Selection", selection.displayName());
        assertFalse(selection.description().isBlank());
        assertTrue(selection.params().isEmpty());
    }

    private static EvaluatedSolution<String> evaluated(String value, double fitness) {
        return new EvaluatedSolution<>(value, fitness);
    }
}