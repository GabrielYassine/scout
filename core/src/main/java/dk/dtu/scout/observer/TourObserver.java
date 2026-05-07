package dk.dtu.scout.observer;

import dk.dtu.scout.State;
import dk.dtu.scout.datatypes.StateKeys;
import dk.dtu.scout.datatypes.TSPInstance;
import dk.dtu.scout.datatypes.VRPInstance;
import dk.dtu.scout.dto.Parameter;
import dk.dtu.scout.logging.IterationSnapshot;
import dk.dtu.scout.logging.RunLog;
import dk.dtu.scout.logging.SeriesMode;
import dk.dtu.scout.problems.TSP;
import dk.dtu.scout.problems.VRP;
import dk.dtu.scout.util.TourVisualizationMapper;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Observer that tracks the current TSP/VRP tour for visualization purposes.
 * It supports both TSP and VRP problems by mapping their respective solution representations to a common format suitable for visualization.
 * For TSP, it expects a single route represented as an array of city indices, while for VRP,
 * it handles multiple routes represented as a list of lists of customer indices.
 * The observer also has an optional feature to log the pheromone matrix as a heatmap if the underlying algorithm uses pheromones.
 * @author s235257
 */
@Component
@Scope("prototype")
public class TourObserver implements Observer<Object> {

    private enum RouteMode {
        TSP,
        VRP
    }

    private record TourSnapshot(List<List<Integer>> routes, double length) {
    }

    private RouteMode routeMode;
    private List<Map<String, Object>> cities;
    private boolean citiesLogged;
    private TSPInstance tspInstance;
    private VRPInstance vrpInstance;
    private State state;
    private boolean includePheromone;

    @Override
    public String id() {
        return "tour";
    }

    @Override
    public String displayName() {
        return "Tour Observer";
    }

    @Override
    public String description() {
        return "Tracks TSP/VRP tours for visualization";
    }

    @Override
    public List<Parameter> params() {
        return List.of(
            new Parameter(
                "includePheromone",
                "Include Pheromone Heatmap",
                "boolean",
                includePheromone,
                null,
                null,
                null
            )
        );
    }

    @Override
    public List<String> supportedSearchSpaces() {
        return List.of("permutation", "route-list");
    }

    @Override
    public void configure(Map<String, Object> params) {
        if (params.containsKey("includePheromone")) {
            includePheromone = (boolean) params.get("includePheromone");
        }
    }

    @Override
    public void init(State state) {
        this.state = state;

        switch (state.get(StateKeys.PROBLEM)) {
            case TSP tsp -> {
                routeMode = RouteMode.TSP;
                tspInstance = tsp.getInstance();
                cities = TourVisualizationMapper.buildTspCities(tspInstance);
            }
            case VRP vrp -> {
                routeMode = RouteMode.VRP;
                vrpInstance = vrp.getInstance();
                cities = TourVisualizationMapper.buildVrpCities(vrpInstance);
            }
            default -> throw new IllegalStateException("TourObserver requires a TSP or VRP problem");
        }
    }

    @Override
    public void onStart(IterationSnapshot<Object> state, RunLog log) {
        if (!citiesLogged) {
            log.putSeries("tspCities", cities, SeriesMode.LATEST_ONLY);
            citiesLogged = true;
        }

        logTourSnapshot(state, log);
        logPheromoneHeatmap(log);
    }

    @Override
    public void onStep(IterationSnapshot<Object> state, RunLog log) {
        logTourSnapshot(state, log);
        logPheromoneHeatmap(log);
    }

    private void logTourSnapshot(IterationSnapshot<Object> state, RunLog log) {
        TourSnapshot snapshot = tourSnapshot(state.currentSolution());

        Map<String, Object> tourData = new HashMap<>();
        tourData.put("tour", snapshot.routes());
        tourData.put("length", snapshot.length());

        log.putSeries("tspTour", tourData, SeriesMode.LATEST_ONLY);
    }

    private TourSnapshot tourSnapshot(Object solution) {
        if (solution instanceof int[] tour) {
            return new TourSnapshot(
                TourVisualizationMapper.wrapTour(tour),
                tspInstance.getTourLength(tour)
            );
        }

        List<List<Integer>> routes = coerceRoutes((List<?>) solution);

        return switch (routeMode) {
            case VRP -> new TourSnapshot(
                TourVisualizationMapper.shiftRoutesForDepot(routes),
                totalDistance(routes)
            );
            case TSP -> new TourSnapshot(
                routes,
                tspInstance.getTourLength(toIntArray(routes.getFirst()))
            );
        };
    }

    @SuppressWarnings("unchecked")
    private List<List<Integer>> coerceRoutes(List<?> raw) {
        if (raw.getFirst() instanceof Number) {
            return List.of(copyRoute(raw));
        }

        return (List<List<Integer>>) raw;
    }

    private List<Integer> copyRoute(List<?> raw) {
        List<Integer> copy = new ArrayList<>(raw.size());

        for (Object value : raw) {
            copy.add(((Number) value).intValue());
        }

        return copy;
    }

    private int[] toIntArray(List<Integer> route) {
        int[] result = new int[route.size()];

        for (int i = 0; i < route.size(); i++) {
            result[i] = route.get(i);
        }

        return result;
    }

    private double totalDistance(List<List<Integer>> routes) {
        double sum = 0.0;

        for (List<Integer> route : routes) {
            sum += routeDistance(route);
        }

        return sum;
    }

    private double routeDistance(List<Integer> route) {
        double distance = 0.0;
        int previousNode = 0;

        for (int customerIndex : route) {
            int nodeIndex = customerIndex + 1;
            distance += vrpInstance.getDistance(previousNode, nodeIndex);
            previousNode = nodeIndex;
        }

        distance += vrpInstance.getDistance(previousNode, 0);
        return distance;
    }

    private void logPheromoneHeatmap(RunLog log) {
        if (!includePheromone) {
            return;
        }

        Object pheromone = state.get(StateKeys.PHEROMONE_MATRIX);
        List<List<Double>> matrix = pheromone instanceof double[][] pheromoneMatrix ? toMatrixList(pheromoneMatrix) : zeroMatrixFromDimension();
        log.putSeries("pheromoneHeatmap", matrix, SeriesMode.LATEST_ONLY);
    }

    private List<List<Double>> toMatrixList(double[][] matrix) {
        List<List<Double>> matrixList = new ArrayList<>(matrix.length);

        for (double[] row : matrix) {
            List<Double> rowList = new ArrayList<>(row.length);

            for (double value : row) {
                rowList.add(value);
            }

            matrixList.add(rowList);
        }

        return matrixList;
    }

    private List<List<Double>> zeroMatrixFromDimension() {
        int dimension = (int) state.get(StateKeys.DIMENSION);

        List<List<Double>> matrix = new ArrayList<>(dimension);

        for (int i = 0; i < dimension; i++) {
            List<Double> row = new ArrayList<>(dimension);

            for (int j = 0; j < dimension; j++) {
                row.add(0.0);
            }

            matrix.add(row);
        }

        return matrix;
    }
}