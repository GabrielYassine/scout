package dk.dtu.scout.problems;

import dk.dtu.scout.Parameter;
import dk.dtu.scout.VRPInstance;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Scope("prototype")
public class VRPProblem implements Problem<int[]> {

    private static final int ROUTE_SEPARATOR = -1;
    private static final double VEHICLE_PENALTY = 1_000_000.0;
    private static final double CAPACITY_PENALTY = 1_000_000.0;

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
        return "CVRP with separator-based route encoding and capacity constraints";
    }

    @Override
    public List<Parameter> params() {
        return List.of();
    }

    @Override
    public List<String> supportedSearchSpaces() {
        return List.of("permutation");
    }

    @Override
    public void configure(Map<String, Object> params) {
        if (params == null) return;

        if (params.containsKey("vrpInstance")) {
            Object vrpInstanceObj = params.get("vrpInstance");
            if (vrpInstanceObj instanceof Map<?, ?>) {
                @SuppressWarnings("unchecked")
                Map<String, Object> vrpInstanceMap = (Map<String, Object>) vrpInstanceObj;
                this.instance = convertMapToInstance(vrpInstanceMap);
            }
        } else if (params.containsKey("instance")) {
            this.instance = (VRPInstance) params.get("instance");
        }
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

    public void setInstance(VRPInstance instance) {
        this.instance = instance;
    }

    public VRPInstance getInstance() {
        return instance;
    }

    @Override
    public double fitness(int[] encodedSolution) {
        if (instance == null) {
            throw new IllegalStateException(
                    "VRP instance not configured. Make sure to upload or configure a VRP instance before running."
            );
        }

        validateEncodedSolution(encodedSolution);

        DecodedVRPSolution decoded = decode(encodedSolution);

        double totalDistance = decoded.totalDistance();
        double capacityPenalty = capacityPenalty(decoded.routes());
        double vehiclePenalty = vehiclePenalty(decoded.routes());

        return -(totalDistance + capacityPenalty + vehiclePenalty);
    }

    /**
     * Decodes a separator-based CVRP solution.
     * Example:
     * [1,2,-1,3,-1,4,5] -> [[1,2], [3], [4,5]]
     */
    public DecodedVRPSolution decode(int[] encodedSolution) {
        List<List<Integer>> routes = decodeRoutes(encodedSolution);
        double totalDistance = totalDistance(routes);
        return new DecodedVRPSolution(routes, totalDistance);
    }

    /**
     * Decodes routes explicitly from separator markers.
     * Repeated separators are allowed and simply ignored.
     * Leading/trailing separators are also ignored.
     */
    public List<List<Integer>> decodeRoutes(int[] encodedSolution) {
        validateEncodedSolution(encodedSolution);

        List<List<Integer>> routes = new ArrayList<>();
        List<Integer> currentRoute = new ArrayList<>();

        for (int value : encodedSolution) {
            if (value == ROUTE_SEPARATOR) {
                if (!currentRoute.isEmpty()) {
                    routes.add(currentRoute);
                    currentRoute = new ArrayList<>();
                }
            } else {
                currentRoute.add(value);
            }
        }

        if (!currentRoute.isEmpty()) {
            routes.add(currentRoute);
        }

        return routes;
    }

    public double routeDistance(List<Integer> route) {
        if (route.isEmpty()) {
            return 0.0;
        }

        double distance = 0.0;
        int previousNode = 0; // depot

        for (int customerIndex : route) {
            int nodeIndex = customerIndex + 1;
            distance += instance.getDistance(previousNode, nodeIndex);
            previousNode = nodeIndex;
        }

        distance += instance.getDistance(previousNode, 0); // return to depot
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

    private void validateEncodedSolution(int[] encodedSolution) {
        if (encodedSolution == null) {
            throw new IllegalArgumentException("Solution cannot be null");
        }

        boolean[] seen = new boolean[instance.getCustomerCount()];
        int seenCustomers = 0;

        for (int value : encodedSolution) {
            if (value == ROUTE_SEPARATOR) {
                continue;
            }

            if (value < 0 || value >= instance.getCustomerCount()) {
                throw new IllegalArgumentException("Customer index out of range: " + value);
            }

            if (seen[value]) {
                throw new IllegalArgumentException("Duplicate customer index in encoded solution: " + value);
            }

            seen[value] = true;
            seenCustomers++;
        }

        if (seenCustomers != instance.getCustomerCount()) {
            throw new IllegalArgumentException(
                    "Encoded solution must contain each customer exactly once. Found "
                            + seenCustomers + " of " + instance.getCustomerCount()
            );
        }

        for (int customerIndex = 0; customerIndex < instance.getCustomerCount(); customerIndex++) {
            double demand = instance.getDemand(customerIndex);
            if (demand > instance.getCapacity()) {
                throw new IllegalArgumentException(
                        "Customer demand exceeds vehicle capacity for customer "
                                + customerIndex + ": " + demand
                );
            }
        }
    }

    public static final class DecodedVRPSolution {
        private final List<List<Integer>> routes;
        private final double totalDistance;

        private DecodedVRPSolution(List<List<Integer>> routes, double totalDistance) {
            this.routes = routes;
            this.totalDistance = totalDistance;
        }

        public List<List<Integer>> routes() {
            return routes;
        }

        public double totalDistance() {
            return totalDistance;
        }
    }
}