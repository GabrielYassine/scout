package dk.dtu.scout.observer;

import dk.dtu.scout.Parameter;
import dk.dtu.scout.State;
import dk.dtu.scout.StateKeys;
import dk.dtu.scout.datatypes.VRPInstance;
import dk.dtu.scout.logging.RunLog;
import dk.dtu.scout.logging.RunState;
import dk.dtu.scout.logging.SeriesMode;
import dk.dtu.scout.problems.VRPProblem;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Scope("prototype")
public class VRPTourObserver implements Observer<List<List<Integer>>> {

    private final List<List<List<Integer>>> routeHistory = new ArrayList<>();
    private List<Map<String, Object>> cities;
    private boolean citiesLogged = false;
    private VRPInstance instance;

    @Override
    public String id() {
        return "vrp-tour";
    }

    @Override
    public String displayName() {
        return "VRP Tour Observer";
    }

    @Override
    public String description() {
        return "Tracks VRP routes for route-list visualization";
    }

    @Override
    public List<Parameter> params() {
        return List.of();
    }

    @Override
    public void init(State state) {
        if (state == null) {
            return;
        }
        Object problemObj = state.get(StateKeys.PROBLEM);
        if (problemObj instanceof VRPProblem vrpProblem) {
            this.instance = vrpProblem.getInstance();
            if (instance != null) {
                this.cities = buildCities(instance);
            }
        }
    }

    @Override
    public void onStart(RunState<List<List<Integer>>> state, RunLog log) {
        if (!citiesLogged && cities != null) {
            log.putSeries("tspCities", cities, SeriesMode.LATEST_ONLY);
            citiesLogged = true;
        }
        logRouteSnapshot(state, log);
    }

    @Override
    public void onStep(RunState<List<List<Integer>>> state, RunLog log) {
        logRouteSnapshot(state, log);
    }

    private void logRouteSnapshot(RunState<List<List<Integer>>> state, RunLog log) {
        List<List<Integer>> routes;
        try {
            routes = state.currentSolution();
            if (routes == null) {
                routes = state.bestSolution();
            }
        } catch (ClassCastException ex) {
            return;
        }
        if (routes == null || routes.isEmpty()) {
            return;
        }

        List<List<Integer>> copied = copyRoutes(routes);
        routeHistory.add(copied);

        List<List<Integer>> shiftedRoutes = shiftRoutesForDepot(copied);

        Map<String, Object> tourData = new HashMap<>();
        tourData.put("tour", shiftedRoutes);

        if (instance != null) {
            double totalDistance = totalDistance(copied, instance);
            tourData.put("length", totalDistance);
        }

        log.putSeries("tspTour", tourData, SeriesMode.LATEST_ONLY);
    }

    private List<Map<String, Object>> buildCities(VRPInstance instance) {
        List<Map<String, Object>> cities = new ArrayList<>();

        double[] depot = instance.getDepotCoordinates();
        Map<String, Object> depotMap = new HashMap<>();
        depotMap.put("x", depot[0]);
        depotMap.put("y", depot[1]);
        depotMap.put("isDepot", true);
        cities.add(depotMap);

        double[][] customers = instance.getCustomerCoordinates();
        for (double[] customer : customers) {
            Map<String, Object> city = new HashMap<>();
            city.put("x", customer[0]);
            city.put("y", customer[1]);
            cities.add(city);
        }

        return cities;
    }

    private List<List<Integer>> copyRoutes(List<List<Integer>> routes) {
        List<List<Integer>> copy = new ArrayList<>(routes.size());
        for (List<Integer> route : routes) {
            if (route == null) {
                copy.add(List.of());
                continue;
            }
            copy.add(new ArrayList<>(route));
        }
        return copy;
    }

    private List<List<Integer>> shiftRoutesForDepot(List<List<Integer>> routes) {
        List<List<Integer>> shifted = new ArrayList<>(routes.size());
        for (List<Integer> route : routes) {
            if (route == null || route.isEmpty()) {
                shifted.add(List.of());
                continue;
            }
            List<Integer> routeShifted = new ArrayList<>(route.size());
            for (Integer idx : route) {
                if (idx == null) {
                    continue;
                }
                routeShifted.add(idx + 1);
            }
            shifted.add(routeShifted);
        }
        return shifted;
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
}
