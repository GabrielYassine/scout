package dk.dtu.scout.util;

import dk.dtu.scout.datatypes.TSPInstance;
import dk.dtu.scout.datatypes.VRPInstance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TourVisualizationMapper {

    private TourVisualizationMapper() {
    }

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