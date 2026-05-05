package dk.dtu.scout.searchSpace;

import dk.dtu.scout.State;
import dk.dtu.scout.datatypes.StateKeys;
import dk.dtu.scout.dto.Parameter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Search space for route-list encoded solutions.
 * A solution is a list of routes, where each route is an ordered list of customer indices.
 */
@Component
@Scope("prototype")
public class RouteList implements SearchSpace<List<List<Integer>>> {

    private int n = 10;
    private int routeCount = 1;

    @Override
    public void init(State state) {
        state.update(Map.of(StateKeys.DIMENSION, n));
    }

    @Override
    public int dimension() {
        return n;
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
        return List.of(
            new Parameter("n", "Customer count (n)", "int", n, 1.0, null),
            new Parameter("routeCount", "Initial route count", "int", routeCount, 1.0, null)
        );
    }

    @Override
    public void configure(Map<String, Object> params) {
        int customerCount = ((Number) params.get("n")).intValue();
        if (customerCount <= 0) {
            throw new IllegalArgumentException("Route-list customer count must be positive");
        }
        this.n = customerCount;

        if (params.containsKey("routeCount")) {
            int configuredRouteCount = ((Number) params.get("routeCount")).intValue();
            if (configuredRouteCount <= 0) {
                throw new IllegalArgumentException("Route count must be positive");
            }
            this.routeCount = configuredRouteCount;
        }
    }

    @Override
    public List<List<Integer>> randomSolution(Random rng) {
        List<Integer> customers = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            customers.add(i);
        }

        Collections.shuffle(customers, rng);

        int routes = Math.min(routeCount, n);
        if (routes == 1) {
            return List.of(new ArrayList<>(customers));
        }

        List<Integer> cutPoints = new ArrayList<>();
        for (int i = 1; i < n; i++) {
            cutPoints.add(i);
        }

        Collections.shuffle(cutPoints, rng);
        cutPoints = cutPoints.subList(0, routes - 1);
        Collections.sort(cutPoints);

        List<List<Integer>> result = new ArrayList<>(routes);
        int start = 0;

        for (int cut : cutPoints) {
            result.add(new ArrayList<>(customers.subList(start, cut)));
            start = cut;
        }

        result.add(new ArrayList<>(customers.subList(start, customers.size())));

        return result;
    }
}