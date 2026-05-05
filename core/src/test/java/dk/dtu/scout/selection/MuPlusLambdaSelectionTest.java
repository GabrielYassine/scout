package dk.dtu.scout.selection;

import dk.dtu.scout.dto.EvaluatedSolution;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class MuPlusLambdaSelectionTest {

    @Test
    void select_returnsBestMuFromParentsAndChildrenCombined() {
        MuPlusLambdaSelection<String> selection = new MuPlusLambdaSelection<>();

        List<EvaluatedSolution<String>> parents = List.of(evaluated("parent-low", 1.0), evaluated("parent-high", 5.0));

        List<EvaluatedSolution<String>> children = List.of(evaluated("child-best", 10.0), evaluated("child-mid", 3.0));

        List<EvaluatedSolution<String>> selected = selection.select(
            parents,
            children,
            2,
            0,
            new Random(1234L)
        );

        assertEquals(2, selected.size());
        assertEquals("child-best", selected.get(0).value());
        assertEquals("parent-high", selected.get(1).value());
    }

    @Test
    void select_includesParentsWhenTheyAreBetterThanChildren() {
        MuPlusLambdaSelection<String> selection = new MuPlusLambdaSelection<>();

        List<EvaluatedSolution<String>> parents = List.of(evaluated("parent-best", 10.0), evaluated("parent-second", 8.0));

        List<EvaluatedSolution<String>> children = List.of(evaluated("child-low", 1.0), evaluated("child-mid", 2.0));

        List<EvaluatedSolution<String>> selected = selection.select(
            parents,
            children,
            2,
            0,
            new Random(1234L)
        );

        assertEquals(List.of("parent-best", "parent-second"), selected.stream().map(EvaluatedSolution::value).toList());
    }

    @Test
    void select_returnsAllCandidatesWhenMuExceedsCandidateCount() {
        MuPlusLambdaSelection<String> selection = new MuPlusLambdaSelection<>();

        List<EvaluatedSolution<String>> selected = selection.select(
            List.of(evaluated("parent", 1.0)),
            List.of(evaluated("child", 2.0)),
            10,
            0,
            new Random(1234L)
        );

        assertEquals(2, selected.size());
        assertEquals("child", selected.get(0).value());
        assertEquals("parent", selected.get(1).value());
    }

    @Test
    void select_returnsEmptyListWhenThereAreNoCandidates() {
        MuPlusLambdaSelection<String> selection = new MuPlusLambdaSelection<>();

        List<EvaluatedSolution<String>> selected = selection.select(
            List.of(),
            List.of(),
            2,
            0,
            new Random(1234L)
        );

        assertTrue(selected.isEmpty());
    }

    @Test
    void metadata_isStable() {
        MuPlusLambdaSelection<String> selection = new MuPlusLambdaSelection<>();

        assertEquals("mu-plus-lambda", selection.id());
        assertEquals("(mu+lambda) Selection", selection.displayName());
        assertFalse(selection.description().isBlank());
        assertTrue(selection.params().isEmpty());
    }

    private static EvaluatedSolution<String> evaluated(String value, double fitness) {
        return new EvaluatedSolution<>(value, fitness);
    }
}