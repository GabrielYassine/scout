package dk.dtu.scout.searchSpace;

import dk.dtu.scout.Parameter;
import dk.dtu.scout.State;
import dk.dtu.scout.StateKeys;
import dk.dtu.scout.datatypes.VRPInstance;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Search space for VRP solutions represented as a list of routes.
 */
@Component
@Scope("prototype")
public class RouteList implements SearchSpace<List<List<Integer>>> {

    private int customerCount = 10;
    private int numberOfVehicles = 1;
    private VRPInstance instance;

    @Override
    public void init(State state) {
        if (state != null) {
            state.update(Map.of(StateKeys.DIMENSION, customerCount));
        }
    }

    @Override
    public int dimension() {
        return customerCount;
    }

    @Override
    public String id() {
        return "route-list";
    }

    @Override
    public String displayName() {
        return "Route List";
    }

    @Override
    public String description() {
        return "Route-list representation, where each route is an ordered customer list";
    }

    @Override
    public List<Parameter> params() {
        return List.of();
    }

    @Override
    public void configure(Map<String, Object> params) {
        if (params == null) return;

        if (params.containsKey("n")) {
            int value = ((Number) params.get("n")).intValue();
            if (value <= 0) {
                throw new IllegalArgumentException("n must be positive");
            }
            this.customerCount = value;
        }

        if (params.containsKey("vrpInstance")) {
            Object vrpInstanceObj = params.get("vrpInstance");
            if (vrpInstanceObj instanceof VRPInstance vrpInstance) {
                this.instance = vrpInstance;
            } else if (vrpInstanceObj instanceof Map<?, ?>) {
                @SuppressWarnings("unchecked")
                Map<String, Object> vrpInstanceMap = (Map<String, Object>) vrpInstanceObj;
                this.instance = convertMapToInstance(vrpInstanceMap);
            }
        }

        if (instance != null) {
            this.customerCount = instance.getCustomerCount();
            this.numberOfVehicles = instance.getNumberOfVehicles();
        }
    }

    @Override
    public List<List<Integer>> randomSolution(Random rng) {
        if (customerCount <= 0) {
            throw new IllegalStateException("customerCount must be positive");
        }

        List<Integer> customers = new ArrayList<>(customerCount);
        for (int i = 0; i < customerCount; i++) {
            customers.add(i);
        }
        Collections.shuffle(customers, rng);

        int routeCount = Math.max(1, Math.min(numberOfVehicles, customerCount));
        if (routeCount == 1) {
            return List.of(new ArrayList<>(customers));
        }

        List<Integer> cutPoints = new ArrayList<>();
        for (int i = 1; i < customerCount; i++) {
            cutPoints.add(i);
        }
        Collections.shuffle(cutPoints, rng);
        cutPoints = cutPoints.subList(0, routeCount - 1);
        Collections.sort(cutPoints);

        List<List<Integer>> routes = new ArrayList<>(routeCount);
        int start = 0;
        for (int cut : cutPoints) {
            routes.add(new ArrayList<>(customers.subList(start, cut)));
            start = cut;
        }
        routes.add(new ArrayList<>(customers.subList(start, customers.size())));

        return routes;
    }

    private VRPInstance convertMapToInstance(Map<String, Object> vrpInstanceMap) {
        String name = (String) vrpInstanceMap.getOrDefault("name", "Custom VRP Instance");
        double capacity = toDouble(vrpInstanceMap.get("capacity"));

        @SuppressWarnings("unchecked")
        Map<String, Object> depotMap = (Map<String, Object>) vrpInstanceMap.get("depot");
        if (depotMap == null) {
            throw new IllegalArgumentException("VRP instance must have a depot");
        }

        double[] depotCoordinates = new double[] {
                toDouble(depotMap.get("x")),
                toDouble(depotMap.get("y"))
        };

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> customersList = (List<Map<String, Object>>) vrpInstanceMap.get("customers");
        if (customersList == null || customersList.isEmpty()) {
            throw new IllegalArgumentException("VRP instance must have customers");
        }

        int customerCount = customersList.size();
        double[][] customerCoordinates = new double[customerCount][2];
        double[] customerDemands = new double[customerCount];

        for (int i = 0; i < customerCount; i++) {
            Map<String, Object> customer = customersList.get(i);
            customerCoordinates[i][0] = toDouble(customer.get("x"));
            customerCoordinates[i][1] = toDouble(customer.get("y"));
            customerDemands[i] = toDouble(customer.getOrDefault("demand", 0.0));
        }

        int numberOfVehicles = (int) toDouble(
                vrpInstanceMap.getOrDefault("numberOfVehicles", customerCount)
        );

        return new VRPInstance(
                name,
                depotCoordinates,
                customerCoordinates,
                customerDemands,
                capacity,
                numberOfVehicles
        );
    }

    private double toDouble(Object value) {
        return value instanceof Number ? ((Number) value).doubleValue() : 0.0;
    }
}
