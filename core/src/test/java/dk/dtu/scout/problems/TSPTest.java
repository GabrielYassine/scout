package dk.dtu.scout.problems;

import dk.dtu.scout.datatypes.TSPInstance;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TSPTest {

    @Test
    void configure_setsTspInstance() {
        TSP problem = new TSP();
        TSPInstance instance = tspInstance();
        problem.configure(Map.of("tspInstance", instance));
        assertSame(instance, problem.getInstance());
    }

    @Test
    void fitness_returnsNegativeTourLength() {
        TSP problem = new TSP();
        problem.configure(Map.of("tspInstance", tspInstance()));
        double fitness = problem.fitness(new int[] {0, 1, 2});
        assertEquals(-16.0, fitness, 1e-9);
    }

    @Test
    void fitness_rejectsUnconfiguredProblem() {
        TSP problem = new TSP();
        assertThrows(IllegalStateException.class, () -> problem.fitness(new int[]{0, 1, 2}));
    }

    @Test
    void fitness_rejectsNullTour() {
        TSP problem = new TSP();
        problem.configure(Map.of("tspInstance", tspInstance()));
        assertThrows(IllegalArgumentException.class, () -> problem.fitness(null));
    }

    @Test
    void fitness_rejectsTourThatIsTooShort() {
        TSP problem = new TSP();
        problem.configure(Map.of("tspInstance", tspInstance()));
        assertThrows(IllegalArgumentException.class, () -> problem.fitness(new int[]{0, 1}));
    }

    @Test
    void fitness_rejectsTourThatIsTooLong() {
        TSP problem = new TSP();
        problem.configure(Map.of("tspInstance", tspInstance()));
        assertThrows(IllegalArgumentException.class, () -> problem.fitness(new int[]{0, 1, 2, 3}));
    }

    @Test
    void isOptimal_returnsFalseWhenInstanceHasNoKnownOptimum() {
        TSP problem = new TSP();
        problem.configure(Map.of("tspInstance", tspInstance()));
        assertFalse(problem.isOptimal(-16.0));
    }

    @Test
    void isOptimal_returnsTrueWhenKnownOptimumIsReached() {
        TSP problem = new TSP();
        problem.configure(Map.of("tspInstance", knownOptimumInstance()));
        assertTrue(problem.isOptimal(-7542.0));
    }

    @Test
    void isOptimal_returnsFalseWhenKnownOptimumIsNotReached() {
        TSP problem = new TSP();
        problem.configure(Map.of("tspInstance", knownOptimumInstance()));
        assertFalse(problem.isOptimal(-7542.00001));
    }

    @Test
    void metadata_isStable() {
        TSP problem = new TSP();

        assertEquals("tsp", problem.id());
        assertEquals("Traveling Salesman Problem", problem.displayName());
        assertFalse(problem.description().isBlank());
        assertTrue(problem.params().isEmpty());
        assertEquals(List.of("permutation"), problem.supportedSearchSpaces());
    }

    private static TSPInstance tspInstance() {
        return new TSPInstance(
            "unknown-test-instance",
            "test instance",
            3,
            new double[][] {
                {0.0, 0.0},
                {3.0, 4.0},
                {6.0, 0.0}
            }
        );
    }

    private static TSPInstance knownOptimumInstance() {
        return new TSPInstance(
            "berlin52",
            "known optimum test",
            3,
            new double[][] {
                {0.0, 0.0},
                {3.0, 4.0},
                {6.0, 0.0}
            }
        );
    }
}