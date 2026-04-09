package dk.dtu.scout.searchSpace;

import dk.dtu.scout.dto.Parameter;
import dk.dtu.scout.State;
import dk.dtu.scout.datatypes.StateKeys;
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
            if (!(vrpInstanceObj instanceof VRPInstance vrpInstance)) {
                throw new IllegalArgumentException("vrpInstance must be a VRPInstance");
            }
            this.instance = vrpInstance;
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
}
