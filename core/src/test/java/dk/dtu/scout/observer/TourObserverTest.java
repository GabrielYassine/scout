package dk.dtu.scout.observer;

import dk.dtu.scout.State;
import dk.dtu.scout.datatypes.StateKeys;
import dk.dtu.scout.datatypes.TSPInstance;
import dk.dtu.scout.datatypes.VRPInstance;
import dk.dtu.scout.dto.EvaluatedSolution;
import dk.dtu.scout.logging.IterationSnapshot;
import dk.dtu.scout.logging.RunLog;
import dk.dtu.scout.problems.TSP;
import dk.dtu.scout.problems.VRP;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TourObserverTest {

    @Test
    void onStart_logsTspCitiesAndTourWithLength() {
        TourObserver observer = new TourObserver();
        RunLog log = new RunLog();

        State state = new State();
        state.update(Map.of(StateKeys.PROBLEM, tspProblem()));

        observer.init(state);
        observer.onStart(snapshot(new int[] {0, 1, 2}), log);

        assertTrue(log.getSeries().containsKey("tspCities"));
        assertTrue(log.getSeries().containsKey("tspTour"));

        List<?> cities = (List<?>) log.getSeries().get("tspCities").getValues().getFirst();
        assertEquals(3, cities.size());

        Map<?, ?> tourData = tourData(log);
        assertEquals(List.of(List.of(0, 1, 2)), tourData.get("tour"));
        assertEquals(16.0, (double) tourData.get("length"), 1e-9);
    }

    @Test
    void onStart_logsCitiesOnlyOnce() {
        TourObserver observer = new TourObserver();
        RunLog log = new RunLog();

        State state = new State();
        state.update(Map.of(StateKeys.PROBLEM, tspProblem()));

        observer.init(state);
        observer.onStart(snapshot(new int[] {0, 1, 2}), log);
        observer.onStart(snapshot(new int[] {0, 2, 1}), log);

        assertEquals(1, log.getSeries().get("tspCities").getValues().size());
    }

    @Test
    void onStep_usesBestSolutionWhenCurrentSolutionIsNull() {
        TourObserver observer = new TourObserver();
        RunLog log = new RunLog();

        State state = new State();
        state.update(Map.of(StateKeys.PROBLEM, tspProblem()));

        observer.init(state);

        IterationSnapshot<Object> snapshot = new IterationSnapshot<>(
            0,
            0,
            null,
            new EvaluatedSolution<>((Object) new int[] {0, 1, 2}, 0.0),
            true
        );

        observer.onStep(snapshot, log);

        Map<?, ?> tourData = tourData(log);
        assertEquals(List.of(List.of(0, 1, 2)), tourData.get("tour"));
        assertEquals(16.0, (double) tourData.get("length"), 1e-9);
    }

    @Test
    void onStep_logsListRouteForTsp() {
        TourObserver observer = new TourObserver();
        RunLog log = new RunLog();

        State state = new State();
        state.update(Map.of(StateKeys.PROBLEM, tspProblem()));

        observer.init(state);
        observer.onStep(snapshot(List.of(0, 1, 2)), log);

        Map<?, ?> tourData = tourData(log);

        assertEquals(List.of(List.of(0, 1, 2)), tourData.get("tour"));
        assertEquals(16.0, (double) tourData.get("length"), 1e-9);
    }

    @Test
    void onStep_logsNestedRoutesWithoutProblemLength() {
        TourObserver observer = new TourObserver();
        RunLog log = new RunLog();

        observer.init(new State());

        observer.onStep(snapshot(List.of(List.of(0, 1), List.of(2))), log);

        Map<?, ?> tourData = tourData(log);

        assertEquals(List.of(List.of(0, 1), List.of(2)), tourData.get("tour"));
        assertFalse(tourData.containsKey("length"));
    }

    @Test
    void onStep_logsVrpRoutesShiftedForDepotAndLength() {
        TourObserver observer = new TourObserver();
        RunLog log = new RunLog();

        State state = new State();
        state.update(Map.of(StateKeys.PROBLEM, vrpProblem()));

        observer.init(state);
        observer.onStep(snapshot(List.of(List.of(0, 1))), log);

        Map<?, ?> tourData = tourData(log);

        assertEquals(List.of(List.of(1, 2)), tourData.get("tour"));
        assertEquals(16.0, (double) tourData.get("length"), 1e-9);
    }

    @Test
    void onStart_logsVrpCitiesWithDepotMarker() {
        TourObserver observer = new TourObserver();
        RunLog log = new RunLog();

        State state = new State();
        state.update(Map.of(StateKeys.PROBLEM, vrpProblem()));

        observer.init(state);
        observer.onStart(snapshot(List.of(List.of(0, 1))), log);

        List<?> cities = (List<?>) log.getSeries().get("tspCities").getValues().getFirst();

        assertEquals(3, cities.size());

        Map<?, ?> depot = (Map<?, ?>) cities.getFirst();
        assertEquals(true, depot.get("isDepot"));
        assertEquals(0.0, (double) depot.get("x"), 1e-9);
        assertEquals(0.0, (double) depot.get("y"), 1e-9);
    }

    @Test
    void onStep_ignoresNullSolution() {
        TourObserver observer = new TourObserver();
        RunLog log = new RunLog();

        observer.init(new State());

        IterationSnapshot<Object> snapshot = new IterationSnapshot<>(
            0,
            0,
            null,
            null,
            false
        );

        observer.onStep(snapshot, log);

        assertFalse(log.getSeries().containsKey("tspTour"));
    }

    @Test
    void onStep_ignoresUnsupportedSolutionType() {
        TourObserver observer = new TourObserver();
        RunLog log = new RunLog();

        observer.init(new State());
        observer.onStep(snapshot("not-a-tour"), log);

        assertFalse(log.getSeries().containsKey("tspTour"));
    }

    @Test
    void onStep_ignoresEmptyRoutes() {
        TourObserver observer = new TourObserver();
        RunLog log = new RunLog();

        observer.init(new State());
        observer.onStep(snapshot(List.of(List.of(), List.of())), log);

        assertFalse(log.getSeries().containsKey("tspTour"));
    }

    @Test
    void configure_ignoresNullParamsAndNonBooleanPheromoneFlag() {
        TourObserver observer = new TourObserver();
        RunLog log = new RunLog();

        State state = new State();
        state.update(Map.of(
            StateKeys.DIMENSION, 2,
            StateKeys.PHEROMONE_MATRIX, new double[][] {
                {1.0, 2.0},
                {3.0, 4.0}
            }
        ));

        observer.configure(null);
        observer.configure(Map.of("includePheromone", "true"));
        observer.init(state);

        observer.onStep(snapshot(new int[] {0, 1}), log);

        assertFalse(log.getSeries().containsKey("pheromoneHeatmap"));
    }

    @Test
    void onStep_logsPheromoneHeatmapFromDoubleArray() {
        TourObserver observer = new TourObserver();
        RunLog log = new RunLog();

        State state = new State();
        state.update(Map.of(
            StateKeys.DIMENSION, 2,
            StateKeys.PHEROMONE_MATRIX, new double[][] {
                {1.0, 2.0},
                {3.0, 4.0}
            }
        ));

        observer.configure(Map.of("includePheromone", true));
        observer.init(state);

        observer.onStep(snapshot(new int[] {0, 1}), log);

        assertEquals(
            List.of(List.of(1.0, 2.0), List.of(3.0, 4.0)),
            log.getSeries().get("pheromoneHeatmap").getValues().getFirst()
        );
    }

    @Test
    void onStep_logsPheromoneHeatmapFromNestedList() {
        TourObserver observer = new TourObserver();
        RunLog log = new RunLog();

        State state = new State();
        state.update(Map.of(
            StateKeys.DIMENSION, 2,
            StateKeys.PHEROMONE_MATRIX, List.of(
                List.of(1, 2.5),
                List.of(3L, "ignored")
            )
        ));

        observer.configure(Map.of("includePheromone", true));
        observer.init(state);

        observer.onStep(snapshot(new int[] {0, 1}), log);

        assertEquals(
            List.of(List.of(1.0, 2.5), List.of(3.0)),
            log.getSeries().get("pheromoneHeatmap").getValues().getFirst()
        );
    }

    @Test
    void onStep_logsZeroPheromoneHeatmapWhenNoMatrixExistsButDimensionExists() {
        TourObserver observer = new TourObserver();
        RunLog log = new RunLog();

        State state = new State();
        state.update(Map.of(StateKeys.DIMENSION, 2));

        observer.configure(Map.of("includePheromone", true));
        observer.init(state);

        observer.onStep(snapshot(new int[] {0, 1}), log);

        assertEquals(
            List.of(List.of(0.0, 0.0), List.of(0.0, 0.0)),
            log.getSeries().get("pheromoneHeatmap").getValues().getFirst()
        );
    }

    @Test
    void onStep_doesNotLogPheromoneWhenEnabledButStateIsMissing() {
        TourObserver observer = new TourObserver();
        RunLog log = new RunLog();

        observer.configure(Map.of("includePheromone", true));
        observer.init(null);

        observer.onStep(snapshot(new int[] {0, 1}), log);

        assertFalse(log.getSeries().containsKey("pheromoneHeatmap"));
    }

    @Test
    void metadata_isStable() {
        TourObserver observer = new TourObserver();

        assertEquals("tour", observer.id());
        assertEquals("Tour Observer", observer.displayName());
        assertFalse(observer.description().isBlank());
        assertEquals(1, observer.params().size());
    }

    private static IterationSnapshot<Object> snapshot(Object solution) {
        EvaluatedSolution<Object> evaluated = new EvaluatedSolution<>(solution, 0.0);
        return new IterationSnapshot<>(0, 0, evaluated, evaluated, true);
    }

    private static Map<?, ?> tourData(RunLog log) {
        return (Map<?, ?>) log.getSeries().get("tspTour").getValues().getFirst();
    }

    private static TSP tspProblem() {
        TSP tsp = new TSP();
        tsp.configure(Map.of("tspInstance", tspInstance()));
        return tsp;
    }

    private static TSPInstance tspInstance() {
        return new TSPInstance(
            "triangle",
            "test instance",
            3,
            new double[][] {
                {0.0, 0.0},
                {3.0, 4.0},
                {6.0, 0.0}
            }
        );
    }

    private static VRP vrpProblem() {
        VRP vrp = new VRP();
        vrp.configure(Map.of("vrpInstance", vrpInstance()));
        return vrp;
    }

    private static VRPInstance vrpInstance() {
        return new VRPInstance(
            "vrp-test",
            "test instance",
            new double[] {0.0, 0.0},
            new double[][] {
                {3.0, 4.0},
                {6.0, 0.0}
            },
            new double[] {1.0, 1.0},
            10.0,
            2
        );
    }
}