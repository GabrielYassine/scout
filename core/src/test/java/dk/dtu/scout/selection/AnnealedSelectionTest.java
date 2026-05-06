package dk.dtu.scout.selection;

import dk.dtu.scout.State;
import dk.dtu.scout.datatypes.StateKeys;
import dk.dtu.scout.dto.EvaluatedSolution;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class AnnealedSelectionTest {

    @Test
    void temperatureAt_usesGeometricCoolingAndMinimumTemperature() {
        AnnealedSelection<String> selection = new AnnealedSelection<>();

        selection.configure(Map.of(
            "initialTemperature", 10.0,
            "coolingRate", 0.5,
            "minTemperature", 2.0
        ));

        assertEquals(10.0, selection.temperatureAt(0), 1e-9);
        assertEquals(5.0, selection.temperatureAt(1), 1e-9);
        assertEquals(2.5, selection.temperatureAt(2), 1e-9);
        assertEquals(2.0, selection.temperatureAt(3), 1e-9);
        assertEquals(10.0, selection.temperatureAt(-5), 1e-9);
    }

    @Test
    void select_classicalAnnealingAlwaysAcceptsImprovement() {
        AnnealedSelection<String> selection = new AnnealedSelection<>();

        List<EvaluatedSolution<String>> selected = selection.select(
            List.of(evaluated("parent", 1.0)),
            List.of(evaluated("child", 2.0)),
            1,
            0,
            new FixedDoubleRandom(0.999)
        );

        assertEquals("child", selected.getFirst().value());
    }

    @Test
    void select_classicalAnnealingRejectsWorseMoveWhenRandomIsTooHigh() {
        AnnealedSelection<String> selection = new AnnealedSelection<>();

        selection.configure(Map.of(
            "initialTemperature", 1.0,
            "coolingRate", 1.0,
            "minTemperature", 1.0
        ));

        List<EvaluatedSolution<String>> selected = selection.select(
            List.of(evaluated("parent", 10.0)),
            List.of(evaluated("child", 9.0)),
            1,
            0,
            new FixedDoubleRandom(0.99)
        );

        assertEquals("parent", selected.getFirst().value());
    }

    @Test
    void select_classicalAnnealingAcceptsWorseMoveWhenRandomIsLowEnough() {
        AnnealedSelection<String> selection = new AnnealedSelection<>();

        selection.configure(Map.of(
            "initialTemperature", 1.0,
            "coolingRate", 1.0,
            "minTemperature", 1.0
        ));

        List<EvaluatedSolution<String>> selected = selection.select(
            List.of(evaluated("parent", 10.0)),
            List.of(evaluated("child", 9.0)),
            1,
            0,
            new FixedDoubleRandom(0.0)
        );

        assertEquals("child", selected.getFirst().value());
    }

    @Test
    void select_populationAnnealedSelectsMuDistinctCandidates() {
        AnnealedSelection<String> selection = new AnnealedSelection<>();

        selection.configure(Map.of(
            "initialTemperature", 10.0,
            "coolingRate", 1.0,
            "minTemperature", 1.0
        ));

        List<EvaluatedSolution<String>> selected = selection.select(
            List.of(evaluated("parent-a", 1.0), evaluated("parent-b", 2.0)),
            List.of(evaluated("child-a", 3.0), evaluated("child-b", 4.0)),
            2,
            0,
            new FixedDoubleRandom(0.0, 0.0)
        );

        assertEquals(2, selected.size());
        assertNotSame(selected.get(0), selected.get(1));
    }

    @Test
    void select_populationAnnealedFallsBackToRandomIndexWhenWeightsAreZero() {
        AnnealedSelection<String> selection = new AnnealedSelection<>();

        selection.configure(Map.of(
            "initialTemperature", 1.0,
            "coolingRate", 1.0,
            "minTemperature", 1.0
        ));

        List<EvaluatedSolution<String>> selected = selection.select(
            List.of(
                evaluated("parent-a", Double.POSITIVE_INFINITY),
                evaluated("parent-b", Double.POSITIVE_INFINITY)
            ),
            List.of(evaluated("child", Double.POSITIVE_INFINITY)),
            2,
            0,
            new FixedIndexRandom(2)
        );

        assertEquals(2, selected.size());
        assertEquals("child", selected.getFirst().value());
    }

    @Test
    void select_rejectsInvalidInputs() {
        AnnealedSelection<String> selection = new AnnealedSelection<>();

        assertThrows(IllegalArgumentException.class, () ->
            selection.select(List.of(evaluated("p", 1.0)), List.of(evaluated("c", 2.0)), 0, 0, new Random(1234L))
        );

        assertThrows(IllegalArgumentException.class, () ->
            selection.select(null, List.of(evaluated("c", 2.0)), 1, 0, new Random(1234L))
        );

        assertThrows(IllegalArgumentException.class, () ->
            selection.select(List.of(), List.of(evaluated("c", 2.0)), 1, 0, new Random(1234L))
        );

        assertThrows(IllegalArgumentException.class, () ->
            selection.select(List.of(evaluated("p", 1.0)), null, 1, 0, new Random(1234L))
        );

        assertThrows(IllegalArgumentException.class, () ->
            selection.select(List.of(evaluated("p", 1.0)), List.of(), 1, 0, new Random(1234L))
        );

        assertThrows(IllegalArgumentException.class, () ->
            selection.select(List.of(evaluated("p", 1.0)), List.of(evaluated("c", 2.0)), 3, 0, new Random(1234L))
        );
    }

    @Test
    void configure_rejectsInvalidValues() {
        AnnealedSelection<String> selection = new AnnealedSelection<>();

        assertThrows(IllegalArgumentException.class, () -> selection.configure(Map.of("initialTemperature", 0.0)));
        assertThrows(IllegalArgumentException.class, () -> selection.configure(Map.of("initialTemperature", -1.0)));
        assertThrows(IllegalArgumentException.class, () -> selection.configure(Map.of("coolingRate", 0.0)));
        assertThrows(IllegalArgumentException.class, () -> selection.configure(Map.of("coolingRate", 1.1)));
        assertThrows(IllegalArgumentException.class, () -> selection.configure(Map.of("minTemperature", 0.0)));
        assertThrows(IllegalArgumentException.class, () -> selection.configure(Map.of("minTemperature", -1.0)));
        assertThrows(IllegalArgumentException.class, () -> selection.configure(Map.of("initialTemperature", 1.0, "minTemperature", 2.0)));
    }

    @Test
    void getStateVariables_returnsCurrentTemperatureAfterSelection() {
        AnnealedSelection<String> selection = new AnnealedSelection<>();

        selection.configure(Map.of(
            "initialTemperature", 10.0,
            "coolingRate", 0.5,
            "minTemperature", 1.0
        ));

        selection.select(
            List.of(evaluated("parent", 1.0)),
            List.of(evaluated("child", 2.0)),
            1,
            2,
            new Random(1234L)
        );

        Map<String, Object> variables = selection.getStateVariables(new State());

        assertEquals(2.5, (double) variables.get(StateKeys.TEMPERATURE), 1e-9);
    }

    @Test
    void metadata_isStable() {
        AnnealedSelection<String> selection = new AnnealedSelection<>();

        assertEquals("annealed-selection", selection.id());
        assertEquals("Annealed Selection", selection.displayName());
        assertFalse(selection.description().isBlank());
        assertEquals(3, selection.params().size());
    }

    @Test
    void configure_allowsUpdatingInitialTemperatureWithoutMinTemperature() {
        AnnealedSelection<String> selection = new AnnealedSelection<>();
        selection.configure(Map.of("initialTemperature", 10.0));
        assertEquals(10.0, selection.temperatureAt(0), 1e-9);
    }

    @Test
    void configure_allowsUpdatingCoolingRateWithoutMinTemperature() {
        AnnealedSelection<String> selection = new AnnealedSelection<>();
        selection.configure(Map.of("coolingRate", 0.5));
        assertEquals(5.0, selection.temperatureAt(0), 1e-9);
        assertEquals(2.5, selection.temperatureAt(1), 1e-9);
    }

    @Test
    void configure_allowsUpdatingMinTemperatureWithoutOtherValues() {
        AnnealedSelection<String> selection = new AnnealedSelection<>();
        selection.configure(Map.of("minTemperature", 1.0));
        assertEquals(5.0, selection.temperatureAt(0), 1e-9);
        assertEquals(1.0, selection.temperatureAt(10000), 1e-9);
    }

    @Test
    void select_usesPopulationSelectionWhenMuIsOneButThereAreMultipleParents() {
        AnnealedSelection<String> selection = new AnnealedSelection<>();

        List<EvaluatedSolution<String>> selected = selection.select(
            List.of(
                evaluated("parent-a", 1.0),
                evaluated("parent-b", 2.0)
            ),
            List.of(evaluated("child", 3.0)),
            1,
            0,
            new FixedDoubleRandom(0.999)
        );

        assertEquals(1, selected.size());
    }

    @Test
    void select_usesPopulationSelectionWhenMuAndParentAreSingleButThereAreMultipleChildren() {
        AnnealedSelection<String> selection = new AnnealedSelection<>();

        List<EvaluatedSolution<String>> selected = selection.select(
            List.of(evaluated("parent", 1.0)),
            List.of(
                evaluated("child-a", 2.0),
                evaluated("child-b", 3.0)
            ),
            1,
            0,
            new FixedDoubleRandom(0.999)
        );

        assertEquals(1, selected.size());
    }

    @Test
    void select_populationAnnealedSelectsLaterCandidateWhenThresholdSkipsFirstWeight() {
        AnnealedSelection<String> selection = new AnnealedSelection<>();

        selection.configure(Map.of(
            "initialTemperature", 10.0,
            "coolingRate", 1.0,
            "minTemperature", 1.0
        ));

        List<EvaluatedSolution<String>> selected = selection.select(
            List.of(
                evaluated("first", 1.0),
                evaluated("second", 1.0)
            ),
            List.of(evaluated("third", 1.0)),
            1,
            0,
            new FixedDoubleRandom(0.5)
        );

        assertEquals("second", selected.getFirst().value());
    }

    private static EvaluatedSolution<String> evaluated(String value, double fitness) {
        return new EvaluatedSolution<>(value, fitness);
    }

    private static final class FixedDoubleRandom extends Random {
        private final double[] values;
        private int index;

        private FixedDoubleRandom(double... values) {
            this.values = values;
        }

        @Override
        public double nextDouble() {
            return values[index++ % values.length];
        }
    }

    private static final class FixedIndexRandom extends Random {
        private final int index;

        private FixedIndexRandom(int index) {
            this.index = index;
        }

        @Override
        public int nextInt(int bound) {
            return Math.floorMod(index, bound);
        }
    }
}