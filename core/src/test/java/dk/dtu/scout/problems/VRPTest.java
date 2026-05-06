package dk.dtu.scout.problems;

import dk.dtu.scout.datatypes.VRPInstance;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VRPTest {

    @Test
    void configure_setsVrpInstance() {
        VRP problem = new VRP();
        VRPInstance instance = vrpInstance(10.0, 2);
        problem.configure(Map.of("vrpInstance", instance));
        assertSame(instance, problem.getInstance());
    }

    @Test
    void routeDistance_returnsDepotToCustomersAndBackDistance() {
        VRP problem = configuredProblem(10.0, 2);
        assertEquals(0.0, problem.routeDistance(List.of()), 1e-9);
        assertEquals(16.0, problem.routeDistance(List.of(0, 1)), 1e-9);
    }

    @Test
    void totalDistance_sumsRouteDistances() {
        VRP problem = configuredProblem(10.0, 2);
        double total = problem.totalDistance(List.of(List.of(0), List.of(1)));
        assertEquals(22.0, total, 1e-9);
    }

    @Test
    void routeDemand_sumsCustomerDemands() {
        VRP problem = configuredProblem(10.0, 2);
        assertEquals(3.0, problem.routeDemand(List.of(0, 1)), 1e-9);
    }

    @Test
    void capacityPenalty_isZeroWhenRoutesRespectCapacity() {
        VRP problem = configuredProblem(10.0, 2);
        assertEquals(0.0, problem.capacityPenalty(List.of(List.of(0, 1))), 1e-9);
    }

    @Test
    void capacityPenalty_penalizesOverloadedRoutes() {
        VRP problem = configuredProblem(2.0, 2);
        assertEquals(1_000_000.0, problem.capacityPenalty(List.of(List.of(0, 1))), 1e-9);
    }

    @Test
    void vehiclePenalty_isZeroWhenVehicleLimitIsRespected() {
        VRP problem = configuredProblem(10.0, 2);
        assertEquals(0.0, problem.vehiclePenalty(List.of(List.of(0), List.of(1))), 1e-9);
    }

    @Test
    void vehiclePenalty_penalizesExcessVehicles() {
        VRP problem = configuredProblem(10.0, 1);
        assertEquals(1_000_000.0, problem.vehiclePenalty(List.of(List.of(0), List.of(1))), 1e-9);
    }

    @Test
    void fitness_returnsNegativeDistanceWhenRoutesAreValid() {
        VRP problem = configuredProblem(10.0, 2);
        double fitness = problem.fitness(List.of(List.of(0, 1)));
        assertEquals(-16.0, fitness, 1e-9);
    }

    @Test
    void fitness_includesCapacityPenalty() {
        VRP problem = configuredProblem(2.0, 2);
        double fitness = problem.fitness(List.of(List.of(0, 1)));
        assertEquals(-(16.0 + 1_000_000.0), fitness, 1e-9);
    }

    @Test
    void fitness_includesVehiclePenalty() {
        VRP problem = configuredProblem(10.0, 1);
        double fitness = problem.fitness(List.of(List.of(0), List.of(1)));
        assertEquals(-(22.0 + 1_000_000.0), fitness, 1e-9);
    }

    @Test
    void fitness_ignoresNullAndEmptyRoutesBeforeValidation() {
        VRP problem = configuredProblem(10.0, 2);

        List<List<Integer>> routes = new ArrayList<>();
        routes.add(null);
        routes.add(List.of());
        routes.add(List.of(0, 1));

        double fitness = problem.fitness(routes);
        assertEquals(-16.0, fitness, 1e-9);
    }

    @Test
    void fitness_rejectsUnconfiguredProblem() {
        VRP problem = new VRP();
        assertThrows(IllegalStateException.class, () -> problem.fitness(List.of(List.of(0, 1))));
    }

    @Test
    void fitness_rejectsNullRoutes() {
        VRP problem = configuredProblem(10.0, 2);
        assertThrows(IllegalArgumentException.class, () -> problem.fitness(null));
    }

    @Test
    void fitness_rejectsEmptyRoutesAfterNormalization() {
        VRP problem = configuredProblem(10.0, 2);
        assertThrows(IllegalArgumentException.class, () -> problem.fitness(List.of()));
        assertThrows(IllegalArgumentException.class, () -> problem.fitness(List.of(List.of(), List.of())));
    }

    @Test
    void fitness_rejectsCustomerIndexBelowRange() {
        VRP problem = configuredProblem(10.0, 2);
        assertThrows(IllegalArgumentException.class, () -> problem.fitness(List.of(List.of(-1, 0))));
    }

    @Test
    void fitness_rejectsCustomerIndexAboveRange() {
        VRP problem = configuredProblem(10.0, 2);
        assertThrows(IllegalArgumentException.class, () -> problem.fitness(List.of(List.of(0, 2))));
    }

    @Test
    void fitness_rejectsDuplicateCustomerIndex() {
        VRP problem = configuredProblem(10.0, 2);
        assertThrows(IllegalArgumentException.class, () -> problem.fitness(List.of(List.of(0), List.of(0))));
    }

    @Test
    void fitness_rejectsMissingCustomer() {
        VRP problem = configuredProblem(10.0, 2);
        assertThrows(IllegalArgumentException.class, () -> problem.fitness(List.of(List.of(0))));
    }

    @Test
    void isOptimal_returnsFalseWhenInstanceHasNoKnownOptimum() {
        VRP problem = configuredProblem(10.0, 2);
        assertFalse(problem.isOptimal(-16.0));
    }

    @Test
    void metadata_isStable() {
        VRP problem = new VRP();

        assertEquals("vrp", problem.id());
        assertEquals("Vehicle Routing Problem", problem.displayName());
        assertFalse(problem.description().isBlank());
        assertTrue(problem.params().isEmpty());
        assertEquals(List.of("route-list"), problem.supportedSearchSpaces());
    }

    private static VRP configuredProblem(double capacity, int vehicles) {
        VRP problem = new VRP();
        problem.configure(Map.of("vrpInstance", vrpInstance(capacity, vehicles)));
        return problem;
    }

    private static VRPInstance vrpInstance(double capacity, int vehicles) {
        return new VRPInstance(
            "unknown-vrp-test-instance",
            "test instance",
            new double[] {0.0, 0.0},
            new double[][] {
                {3.0, 4.0},
                {6.0, 0.0}
            },
            new double[] {1.0, 2.0},
            capacity,
            vehicles
        );
    }

    @Test
    void isOptimal_returnsTrueWhenKnownOptimumIsReached() {
        VRP problem = new VRP();
        problem.configure(Map.of("vrpInstance", knownOptimumInstance()));
        assertTrue(problem.isOptimal(-784.0));
    }

    @Test
    void isOptimal_returnsFalseWhenKnownOptimumIsNotReached() {
        VRP problem = new VRP();
        problem.configure(Map.of("vrpInstance", knownOptimumInstance()));
        assertFalse(problem.isOptimal(-784.00001));
    }

    private static VRPInstance knownOptimumInstance() {
        return new VRPInstance(
            "A-n32-k5",
            "known optimum test",
            new double[] {0.0, 0.0},
            new double[][] {
                {3.0, 4.0},
                {6.0, 0.0}
            },
            new double[] {1.0, 2.0},
            10.0,
            2
        );
    }
}