package dk.dtu.scout.observer;

import dk.dtu.scout.dto.Parameter;
import dk.dtu.scout.State;
import dk.dtu.scout.datatypes.StateKeys;
import dk.dtu.scout.datatypes.TSPInstance;
import dk.dtu.scout.datatypes.VRPInstance;
import dk.dtu.scout.logging.RunLog;
import dk.dtu.scout.logging.SeriesMode;
import dk.dtu.scout.problems.TSPProblem;
import dk.dtu.scout.problems.VRPProblem;
import dk.dtu.scout.logging.IterationSnapshot;
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
        if (problemObj instanceof TSPProblem tspProblem) {
            this.tspInstance = tspProblem.getInstance();
            if (tspInstance != null) {
                this.cities = TourObserverSupport.buildTspCities(tspInstance);
            }
        } else if (problemObj instanceof VRPProblem vrpProblem) {
            this.vrpInstance = vrpProblem.getInstance();
            if (vrpInstance != null) {
                this.cities = TourObserverSupport.buildVrpCities(vrpInstance);
            }
        }
    }

    @Override
    public void configure(Map<String, Object> params) {
        if (params == null) {
            return;
        }
        Object pheromoneParam = params.get("includePheromone");
        if (pheromoneParam instanceof Boolean) {
            this.includePheromone = (Boolean) pheromoneParam;
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
            routesToLog = TourObserverSupport.wrapTour(tour);
            if (tspInstance != null) {
                length = tspInstance.getTourLength(tour);
            }
        } else if (solution instanceof List<?> list) {
            List<List<Integer>> routes = coerceRoutes(list);
            if (routes.isEmpty()) {
                return;
            }

            if (vrpInstance != null) {
                routesToLog = TourObserverSupport.shiftRoutesForDepot(routes);
                length = totalDistance(routes, vrpInstance);
            } else {
                routesToLog = routes;
                if (tspInstance != null && routes.size() == 1) {
                    int[] tour = toIntArray(routes.get(0));
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

        Object first = raw.get(0);
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
        if (!includePheromone) return;
        if (this.state == null) return;

        Object pheromoneObj = this.state.get(StateKeys.PHEROMONE_MATRIX);
        List<List<Double>> matrixList = null;

        if (pheromoneObj instanceof double[][] pheromoneMatrix) {
            matrixList = new ArrayList<>(pheromoneMatrix.length);
            for (double[] row : pheromoneMatrix) {
                List<Double> rowList = new ArrayList<>(row.length);
                for (double v : row) rowList.add(v);
                matrixList.add(rowList);
            }
        } else if (pheromoneObj instanceof List<?> outer && !outer.isEmpty() && outer.get(0) instanceof List<?>) {
            matrixList = new ArrayList<>(outer.size());
            for (Object r : outer) {
                if (r instanceof List<?> inner) {
                    List<Double> rowList = new ArrayList<>(inner.size());
                    for (Object v : inner) {
                        if (v instanceof Number n) rowList.add(n.doubleValue());
                    }
                    matrixList.add(rowList);
                }
            }
        } else {
            int dim = 0;
            if (tspInstance != null) dim = tspInstance.getDimension();
            if (dim <= 0) {
                Object dimObj = this.state.get(StateKeys.DIMENSION);
                if (dimObj instanceof Number n) dim = n.intValue();
            }
            if (dim > 0) {
                matrixList = new ArrayList<>(dim);
                for (int i = 0; i < dim; i++) {
                    List<Double> row = new ArrayList<>(dim);
                    for (int j = 0; j < dim; j++) row.add(0.0);
                    matrixList.add(row);
                }
            }
        }

        if (matrixList != null && !matrixList.isEmpty()) {
            log.putSeries("pheromoneHeatmap", matrixList, SeriesMode.LATEST_ONLY);
        }
    }
}
