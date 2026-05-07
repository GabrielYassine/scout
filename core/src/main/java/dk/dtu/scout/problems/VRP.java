package dk.dtu.scout.problems;

import dk.dtu.scout.dto.Parameter;
import dk.dtu.scout.datatypes.VRPInstance;
import dk.dtu.scout.util.OptimaLookup;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author s235257 & s230632
 */
@Component
@Scope("prototype")
public class VRP implements Problem<List<List<Integer>>> {

    private static final double VEHICLE_PENALTY = 1_000_000.0;
    private static final double CAPACITY_PENALTY = 1_000_000.0;
    private static final String VRP_OPTIMA_RESOURCE = "optima/vrp-optima.properties";
    private static final double EPSILON = 1e-9;
    private static final Map<String, Double> VRP_OPTIMA = OptimaLookup.loadDoubleMap(VRP_OPTIMA_RESOURCE);

    private VRPInstance instance;

    @Override
    public String id() {
        return "vrp";
    }

    @Override
    public String displayName() {
        return "Vehicle Routing Problem";
    }

    @Override
    public String description() {
        return "CVRP with route-list encoding and capacity constraints";
    }

    @Override
    public List<Parameter> params() {
        return List.of();
    }

    @Override
    public List<String> supportedSearchSpaces() {
        return List.of("route-list");
    }

    @Override
    public void configure(Map<String, Object> params) {
        this.instance = (VRPInstance) params.get("vrpInstance");
    }

    public VRPInstance getInstance() {
        return instance;
    }

    @Override
    public boolean isOptimal(double fitness) {
        Double optimum = OptimaLookup.resolve(VRP_OPTIMA, instance.getName());
        return optimum != null && fitness >= -optimum - EPSILON;
    }

    @Override
    public double fitness(List<List<Integer>> routes) {
        if (instance == null) {
            throw new IllegalStateException("VRP instance not configured. Make sure to upload or configure a VRP instance before running.");
        }

        List<List<Integer>> normalizedRoutes = normalizeRoutes(routes);
        validateRoutes(normalizedRoutes);

        double totalDistance = totalDistance(normalizedRoutes);
        double capacityPenalty = capacityPenalty(normalizedRoutes);
        double vehiclePenalty = vehiclePenalty(normalizedRoutes);

        return -(totalDistance + capacityPenalty + vehiclePenalty);
    }

    private List<List<Integer>> normalizeRoutes(List<List<Integer>> routes) {
        if (routes == null) {
            return List.of();
        }

        List<List<Integer>> normalized = new ArrayList<>();
        for (List<Integer> route : routes) {
            if (route == null || route.isEmpty()) {
                continue;
            }
            normalized.add(route);
        }
        return normalized;
    }

    public double routeDistance(List<Integer> route) {
        if (route.isEmpty()) {
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

    public double totalDistance(List<List<Integer>> routes) {
        double sum = 0.0;
        for (List<Integer> route : routes) {
            sum += routeDistance(route);
        }
        return sum;
    }

    public double routeDemand(List<Integer> route) {
        double demand = 0.0;
        for (int customerIndex : route) {
            demand += instance.getDemand(customerIndex);
        }
        return demand;
    }

    public double capacityPenalty(List<List<Integer>> routes) {
        double penalty = 0.0;
        double capacity = instance.getCapacity();

        for (List<Integer> route : routes) {
            double overload = routeDemand(route) - capacity;
            if (overload > 0.0) {
                penalty += overload * CAPACITY_PENALTY;
            }
        }

        return penalty;
    }

    public double vehiclePenalty(List<List<Integer>> routes) {
        int excessVehicles = routes.size() - instance.getNumberOfVehicles();
        if (excessVehicles <= 0) {
            return 0.0;
        }
        return excessVehicles * VEHICLE_PENALTY;
    }

    private void validateRoutes(List<List<Integer>> routes) {
        if (routes.isEmpty()) {
            throw new IllegalArgumentException("Routes cannot be empty");
        }

        boolean[] seen = new boolean[instance.getCustomerCount()];
        int seenCustomers = 0;

        for (List<Integer> route : routes) {
            for (int customerIndex : route) {
                if (customerIndex < 0 || customerIndex >= instance.getCustomerCount()) {
                    throw new IllegalArgumentException("Customer index out of range: " + customerIndex);
                }

                if (seen[customerIndex]) {
                    throw new IllegalArgumentException("Duplicate customer index in routes: " + customerIndex);
                }

                seen[customerIndex] = true;
                seenCustomers++;
            }
        }

        if (seenCustomers != instance.getCustomerCount()) {
            throw new IllegalArgumentException("Routes must contain each customer exactly once. Found " + seenCustomers + " of " + instance.getCustomerCount());
        }
    }
}