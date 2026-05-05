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

@Component
@Scope("prototype")
public class TourObserver implements Observer<Object> {

    private List<Map<String, Object>> cities;
    private boolean citiesLogged = false;
    private TSPInstance tspInstance;
    private VRPInstance vrpInstance;
    private State state;
    private boolean includePheromone = false;

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
                        false,
                        null,
                        null,
                        null
                )
        );
    }

    @Override
    public void init(State state) {
        this.state = state;

        if (state == null) {
            return;
        }

        Object problemObj = state.get(StateKeys.PROBLEM);

        if (problemObj instanceof TSP tsp) {
            this.tspInstance = tsp.getInstance();

            if (tspInstance != null) {
                this.cities = TourVisualizationMapper.buildTspCities(tspInstance);
            }
        } else if (problemObj instanceof VRP vrp) {
            this.vrpInstance = vrp.getInstance();

            if (vrpInstance != null) {
                this.cities = TourVisualizationMapper.buildVrpCities(vrpInstance);
            }
        }
    }

    @Override
    public void configure(Map<String, Object> params) {
        if (params == null) {
            return;
        }

        Object pheromoneParam = params.get("includePheromone");
        if (pheromoneParam instanceof Boolean value) {
            this.includePheromone = value;
        }
    }

    @Override
    public void onStart(IterationSnapshot<Object> state, RunLog log) {
        if (!citiesLogged && cities != null) {
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
        Object solution;

        try {
            solution = state.currentSolution();

            if (solution == null) {
                solution = state.bestSolution();
            }
        } catch (ClassCastException ex) {
            return;
        }

        if (solution == null) {
            return;
        }

        Map<String, Object> tourData = new HashMap<>();
        Double length = null;
        List<List<Integer>> routesToLog;

        if (solution instanceof int[] tour) {
            routesToLog = TourVisualizationMapper.wrapTour(tour);

            if (tspInstance != null) {
                length = tspInstance.getTourLength(tour);
            }
        } else if (solution instanceof List<?> list) {
            List<List<Integer>> routes = coerceRoutes(list);

            if (routes.isEmpty()) {
                return;
            }

            if (vrpInstance != null) {
                routesToLog = TourVisualizationMapper.shiftRoutesForDepot(routes);
                length = totalDistance(routes, vrpInstance);
            } else {
                routesToLog = routes;

                if (tspInstance != null && routes.size() == 1) {
                    int[] tour = toIntArray(routes.getFirst());

                    if (tour.length > 0) {
                        length = tspInstance.getTourLength(tour);
                    }
                }
            }
        } else {
            return;
        }

        tourData.put("tour", routesToLog);

        if (length != null) {
            tourData.put("length", length);
        }

        log.putSeries("tspTour", tourData, SeriesMode.LATEST_ONLY);
    }

    private List<List<Integer>> coerceRoutes(List<?> raw) {
        if (raw.isEmpty()) {
            return List.of();
        }

        Object first = raw.getFirst();

        if (first instanceof Number) {
            return List.of(copyRoute(raw));
        }

        List<List<Integer>> routes = new ArrayList<>();

        for (Object routeObj : raw) {
            if (routeObj instanceof List<?> route) {
                List<Integer> copy = copyRoute(route);

                if (!copy.isEmpty()) {
                    routes.add(copy);
                }
            }
        }

        return routes;
    }

    private List<Integer> copyRoute(List<?> raw) {
        List<Integer> copy = new ArrayList<>(raw.size());

        for (Object value : raw) {
            if (value instanceof Number number) {
                copy.add(number.intValue());
            }
        }

        return copy;
    }

    private int[] toIntArray(List<Integer> route) {
        int[] result = new int[route.size()];

        for (int i = 0; i < route.size(); i++) {
            Integer value = route.get(i);
            result[i] = value == null ? 0 : value;
        }

        return result;
    }

    private double totalDistance(List<List<Integer>> routes, VRPInstance instance) {
        double sum = 0.0;

        for (List<Integer> route : routes) {
            sum += routeDistance(route, instance);
        }

        return sum;
    }

    private double routeDistance(List<Integer> route, VRPInstance instance) {
        if (route == null || route.isEmpty()) {
            return 0.0;
        }

        double distance = 0.0;
        int previousNode = 0;

        for (int customerIndex : route) {
            int nodeIndex = customerIndex + 1;
            distance += instance.getDistance(previousNode, nodeIndex);
            previousNode = nodeIndex;
        }

        distance += instance.getDistance(previousNode, 0);
        return distance;
    }

    private void logPheromoneHeatmap(RunLog log) {
        if (!includePheromone || state == null) {
            return;
        }

        Object pheromoneObj = state.get(StateKeys.PHEROMONE_MATRIX);
        List<List<Double>> matrixList = null;

        if (pheromoneObj instanceof double[][] pheromoneMatrix) {
            matrixList = toMatrixList(pheromoneMatrix);
        } else if (pheromoneObj instanceof List<?> outer && !outer.isEmpty() && outer.getFirst() instanceof List<?>) {
            matrixList = toMatrixList(outer);
        } else {
            matrixList = zeroMatrixFromDimension();
        }

        if (matrixList != null && !matrixList.isEmpty()) {
            log.putSeries("pheromoneHeatmap", matrixList, SeriesMode.LATEST_ONLY);
        }
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

    private List<List<Double>> toMatrixList(List<?> outer) {
        List<List<Double>> matrixList = new ArrayList<>(outer.size());

        for (Object rowObj : outer) {
            if (rowObj instanceof List<?> inner) {
                List<Double> rowList = new ArrayList<>(inner.size());

                for (Object value : inner) {
                    if (value instanceof Number number) {
                        rowList.add(number.doubleValue());
                    }
                }

                matrixList.add(rowList);
            }
        }

        return matrixList;
    }

    private List<List<Double>> zeroMatrixFromDimension() {
        int dimension = 0;

        if (tspInstance != null) {
            dimension = tspInstance.getDimension();
        }

        if (dimension <= 0) {
            Object dimensionObj = state.get(StateKeys.DIMENSION);

            if (dimensionObj instanceof Number number) {
                dimension = number.intValue();
            }
        }

        if (dimension <= 0) {
            return null;
        }

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