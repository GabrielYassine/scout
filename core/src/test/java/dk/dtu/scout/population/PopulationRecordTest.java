package dk.dtu.scout.population;

import dk.dtu.scout.ScoutComponent;
import dk.dtu.scout.dto.EvaluatedSolution;
import dk.dtu.scout.dto.Parameter;
import dk.dtu.scout.logging.IterationSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PopulationRecordTest {

    @Test
    void populationInitialization_replacesNullSharedStateVariablesWithEmptyMap() {
        PopulationInitialization<String> initialization = new PopulationInitialization<>(
            new DummyPopulationState<>(),
            snapshot(),
            1,
            null,
            List.of(new DummyComponent())
        );

        assertTrue(initialization.sharedStateVariables().isEmpty());
        assertEquals(1, initialization.stateComponents().size());
    }

    @Test
    void populationInitialization_replacesNullStateComponentsWithEmptyList() {
        PopulationInitialization<String> initialization = new PopulationInitialization<>(
            new DummyPopulationState<>(),
            snapshot(),
            1,
            Map.of("x", 1),
            null
        );

        assertEquals(Map.of("x", 1), initialization.sharedStateVariables());
        assertTrue(initialization.stateComponents().isEmpty());
    }

    @Test
    void populationStepResult_replacesNullSharedStateVariablesWithEmptyMap() {
        PopulationStepResult<String> result = new PopulationStepResult<>(
            snapshot(),
            1,
            null
        );

        assertTrue(result.sharedStateVariables().isEmpty());
    }

    @Test
    void populationStepResult_keepsProvidedSharedStateVariables() {
        PopulationStepResult<String> result = new PopulationStepResult<>(
            snapshot(),
            1,
            Map.of("x", 1)
        );

        assertEquals(Map.of("x", 1), result.sharedStateVariables());
    }

    private static IterationSnapshot<String> snapshot() {
        EvaluatedSolution<String> solution = new EvaluatedSolution<>("solution", 1.0);

        return new IterationSnapshot<>(
            0,
            1,
            solution,
            solution,
            true
        );
    }

    private static final class DummyPopulationState<S> implements PopulationState<S> {}

    private static final class DummyComponent implements ScoutComponent {
        @Override
        public String id() {
            return "dummy";
        }

        @Override
        public String displayName() {
            return "Dummy";
        }

        @Override
        public String description() {
            return "Dummy component";
        }

        @Override
        public List<Parameter> params() {
            return List.of();
        }
    }
}