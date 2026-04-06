package dk.dtu.scout.generator;

import dk.dtu.scout.Parameter;
import dk.dtu.scout.State;
import dk.dtu.scout.StateKeys;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Component
@Scope("prototype")
public class RouteListSwapGenerator implements Generator<List<List<Integer>>> {

    private State state;

    @Override
    public void init(State state) {
        this.state = state;
    }

    @Override
    public String id() {
        return "route-list-swap";
    }

    @Override
    public String displayName() {
        return "Route List Swap";
    }

    @Override
    public String description() {
        return "Swaps two customers within or across route-list solutions";
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
    }

    @Override
    public List<List<Integer>> generate(Random rng) {
        Object baseObj = state.get(StateKeys.OFFSPRING_BASE);
        if (baseObj == null) {
            baseObj = state.get(StateKeys.SELECTED_PARENT_1);
        }
        if (!(baseObj instanceof List<?> baseRoutes)) {
            throw new IllegalStateException("RouteListSwapGenerator requires 'offspringBase' or 'selectedParent1' in state");
        }

        List<List<Integer>> routes = copyRoutes(baseRoutes);
        int totalCustomers = countCustomers(routes);
        if (totalCustomers < 2) {
            return routes;
        }

        RoutePos first = pickPosition(routes, rng);
        RoutePos second = pickPosition(routes, rng);
        int guard = 0;
        while (first.equals(second) && guard < 10) {
            second = pickPosition(routes, rng);
            guard++;
        }

        if (first.equals(second)) {
            return routes;
        }

        int firstValue = routes.get(first.routeIndex).get(first.positionIndex);
        int secondValue = routes.get(second.routeIndex).get(second.positionIndex);

        routes.get(first.routeIndex).set(first.positionIndex, secondValue);
        routes.get(second.routeIndex).set(second.positionIndex, firstValue);

        return routes;
    }

    private int countCustomers(List<List<Integer>> routes) {
        int total = 0;
        for (List<Integer> route : routes) {
            total += route.size();
        }
        return total;
    }

    private RoutePos pickPosition(List<List<Integer>> routes, Random rng) {
        List<Integer> eligibleRoutes = new ArrayList<>();
        for (int i = 0; i < routes.size(); i++) {
            if (!routes.get(i).isEmpty()) {
                eligibleRoutes.add(i);
            }
        }

        if (eligibleRoutes.isEmpty()) {
            return new RoutePos(0, 0);
        }

        int routeIndex = eligibleRoutes.get(rng.nextInt(eligibleRoutes.size()));
        int positionIndex = rng.nextInt(routes.get(routeIndex).size());
        return new RoutePos(routeIndex, positionIndex);
    }

    private List<List<Integer>> copyRoutes(List<?> baseRoutes) {
        List<List<Integer>> routes = new ArrayList<>();
        for (Object routeObj : baseRoutes) {
            if (!(routeObj instanceof List<?> route)) {
                throw new IllegalArgumentException("Route must be a list of integers");
            }
            List<Integer> copy = new ArrayList<>(route.size());
            for (Object value : route) {
                if (!(value instanceof Number number)) {
                    throw new IllegalArgumentException("Customer index must be numeric");
                }
                copy.add(number.intValue());
            }
            routes.add(copy);
        }
        return routes;
    }

    private record RoutePos(int routeIndex, int positionIndex) {
    }
}
