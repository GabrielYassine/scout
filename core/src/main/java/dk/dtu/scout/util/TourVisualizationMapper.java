package dk.dtu.scout.util;

import dk.dtu.scout.datatypes.TSPInstance;
import dk.dtu.scout.datatypes.VRPInstance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *  Utility class for mapping TSP and VRP instances and tours to a format suitable for visualization.
 * @author s235257
 */
public final class TourVisualizationMapper {

    private TourVisualizationMapper() {
    }
    /**
     * Builds a list of city maps for a VRP instance, including the depot and customer locations.
     * @param instance The VRP instance to map
     * @return A list of maps representing the cities for visualization
     */
    public static List<Map<String, Object>> buildVrpCities(VRPInstance instance) {
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
    /**
     * Builds a list of city maps for a TSP instance, including the coordinates of each city.
     * @param instance The TSP instance to map
     * @return A list of maps representing the cities for visualization
     */
    public static List<Map<String, Object>> buildTspCities(TSPInstance instance) {
        List<Map<String, Object>> cities = new ArrayList<>();
        double[][] coords = instance.getCoordinates();

        for (double[] coord : coords) {
            Map<String, Object> city = new HashMap<>();
            city.put("x", coord[0]);
            city.put("y", coord[1]);
            cities.add(city);
        }

        return cities;
    }
    /**
     * Wraps a TSP tour (array of city indices) into a list of routes format expected by the visualization.
     * @param tour An array representing the TSP tour (sequence of city indices)
     * @return A list containing a single route, which is a list of city indices
     */
    public static List<List<Integer>> wrapTour(int[] tour) {
        if (tour == null || tour.length == 0) {
            return List.of();
        }

        List<Integer> route = new ArrayList<>(tour.length);
        for (int city : tour) {
            route.add(city);
        }

        return List.of(route);
    }
    /**
     * Shifts the customer indices in the VRP routes by +1 to account for the depot being at index 0 in the visualization.
     * @param routes A list of routes, where each route is a list of customer indices (0-based)
     * @return A new list of routes with customer indices shifted by +1
     */
    public static List<List<Integer>> shiftRoutesForDepot(List<List<Integer>> routes) {
        List<List<Integer>> shifted = new ArrayList<>(routes.size());

        for (List<Integer> route : routes) {
            if (route == null || route.isEmpty()) {
                shifted.add(List.of());
                continue;
            }

            List<Integer> shiftedRoute = new ArrayList<>(route.size());
            for (Integer customerIndex : route) {
                if (customerIndex != null) {
                    shiftedRoute.add(customerIndex + 1);
                }
            }

            shifted.add(shiftedRoute);
        }

        return shifted;
    }
}