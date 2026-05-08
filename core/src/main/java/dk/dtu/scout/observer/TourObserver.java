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
    /**
     * Initializes the observer by determining the problem type (TSP or VRP) and extracting the relevant instance data for visualization.
     * It maps the city coordinates to a common format used for visualization and stores the problem instance for later use in calculating tour lengths.
     * @param state the current state containing problem information and other relevant data
     */
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
    /**
     * Logs the initial tour and pheromone heatmap (if enabled) at the start of the run.
     * @param state current iteration snapshot containing the initial solution
     * @param log run log where the tour snapshot and pheromone heatmap are stored
     */
    @Override
    public void onStart(IterationSnapshot<Object> state, RunLog log) {
        if (!citiesLogged) {
            log.putSeries("tspCities", cities, SeriesMode.LATEST_ONLY);
            citiesLogged = true;
        }

        logTourSnapshot(state, log);
        logPheromoneHeatmap(log);
    }
    /**
     * Logs the current tour and pheromone heatmap (if enabled) at each evaluation point.
     * @param state current iteration snapshot containing the current solution
     * @param log run log where the tour snapshot and pheromone heatmap are stored
     */
    @Override
    public void onStep(IterationSnapshot<Object> state, RunLog log) {
        logTourSnapshot(state, log);
        logPheromoneHeatmap(log);
    }

    /**
     * Extracts the current tour from the solution, calculates its length, and logs it in a format suitable for visualization.
     * @param state current iteration snapshot containing the current solution
     * @param log run log where the tour snapshot is stored
     */
    private void logTourSnapshot(IterationSnapshot<Object> state, RunLog log) {
        TourSnapshot snapshot = tourSnapshot(state.currentSolution());

        Map<String, Object> tourData = new HashMap<>();
        tourData.put("tour", snapshot.routes());
        tourData.put("length", snapshot.length());

        log.putSeries("tspTour", tourData, SeriesMode.LATEST_ONLY);
    }
    /**
     * Converts the current solution into a common tour snapshot format that includes the routes and their total length.
     * For TSP, it wraps the single route in a list and calculates the tour length using the TSP instance.
     * For VRP, it shifts the routes to include the depot and calculates the total distance using the VRP instance.
     * @param solution the current solution which can be either an int array for TSP or a list of lists for VRP
     * @return a TourSnapshot containing the routes and their total length
     */
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
    /**
     * Coerces the raw solution into a list of routes format. If the first element is a number, it treats the solution as a single route and wraps it in a list.
     * @param raw the raw solution which can be either a single route or a list of routes
     * @return a list of routes where each route is a list of customer indices
     */
    @SuppressWarnings("unchecked")
    private List<List<Integer>> coerceRoutes(List<?> raw) {
        if (raw.getFirst() instanceof Number) {
            return List.of(copyRoute(raw));
        }

        return (List<List<Integer>>) raw;
    }
    /**
     * Creates a copy of the raw route by converting each element to an integer.
     * @param raw the raw route which is a list of numbers representing customer indices
     * @return a list of integers representing the copied route
     */
    private List<Integer> copyRoute(List<?> raw) {
        List<Integer> copy = new ArrayList<>(raw.size());

        for (Object value : raw) {
            copy.add(((Number) value).intValue());
        }

        return copy;
    }
    /**
     * Converts a list of customer indices representing a route into an array of integers.
     * @param route the list of customer indices representing a route
     * @return an array of integers representing the route
     */
    private int[] toIntArray(List<Integer> route) {
        int[] result = new int[route.size()];

        for (int i = 0; i < route.size(); i++) {
            result[i] = route.get(i);
        }

        return result;
    }
    /**
     * Calculates the total distance of a set of routes by summing the distances of each individual route.
     * @param routes a list of routes where each route is a list of customer indices
     * @return the total distance of all routes combined
     */
    private double totalDistance(List<List<Integer>> routes) {
        double sum = 0.0;

        for (List<Integer> route : routes) {
            sum += routeDistance(route);
        }

        return sum;
    }
    /**
     * Calculates the distance of a single route by summing the distances between consecutive nodes, including the return to the depot.
     * It assumes that the route is represented as a list of customer indices and that the depot is represented as node 0.
     * @param route a list of customer indices representing a single route
     * @return the total distance of the route including the return to the depot
     */
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
    /**
     * Logs the current pheromone matrix as a heatmap if the includePheromone parameter is enabled.
     * It retrieves the pheromone matrix from the state, converts it to a list of lists format suitable for visualization,
     * and logs it in the run log. If the pheromone matrix is not available, it logs a zero matrix based on the problem dimension.
     * @param log run log where the pheromone heatmap is stored
     */
    private void logPheromoneHeatmap(RunLog log) {
        if (!includePheromone) {
            return;
        }

        Object pheromone = state.get(StateKeys.PHEROMONE_MATRIX);
        List<List<Double>> matrix = pheromone instanceof double[][] pheromoneMatrix ? toMatrixList(pheromoneMatrix) : zeroMatrixFromDimension();
        log.putSeries("pheromoneHeatmap", matrix, SeriesMode.LATEST_ONLY);
    }

    /**
     * Converts a 2D array of doubles into a list of lists format suitable for visualization.
     * @param matrix the 2D array of doubles representing the pheromone matrix
     * @return a list of lists of doubles representing the same matrix in a format suitable for visualization
     */
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
    /**
     * Creates a zero matrix in a list of lists format based on the problem dimension retrieved from the state.
     * This is used as a fallback when the pheromone matrix is not available in the state.
     * @return a list of lists of doubles representing a zero matrix with dimensions based on the problem dimension
     */
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