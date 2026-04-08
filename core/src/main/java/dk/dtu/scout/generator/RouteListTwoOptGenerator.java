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
public class RouteListTwoOptGenerator implements Generator<List<List<Integer>>> {

    private State state;

    @Override
    public void init(State state) {
        this.state = state;
    }

    @Override
    public String id() {
        return "route-list-2opt";
    }

    @Override
    public String displayName() {
        return "Route List 2-Opt";
    }

    @Override
    public String description() {
        return "Applies a 2-opt reversal within a single route";
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
            throw new IllegalStateException("RouteListTwoOptGenerator requires 'offspringBase' or 'selectedParent1' in state");
        }

        List<List<Integer>> routes = copyRoutes(baseRoutes);
        int routeIndex = pickRouteIndex(routes, rng);
        if (routeIndex < 0) {
            return routes;
        }

        List<Integer> route = routes.get(routeIndex);
        if (route.size() < 2) {
            return routes;
        }

        int i = rng.nextInt(route.size() - 1);
        int k = i + 1 + rng.nextInt(route.size() - i - 1);

        while (i < k) {
            Integer tmp = route.get(i);
            route.set(i, route.get(k));
            route.set(k, tmp);
            i++;
            k--;
        }

        return routes;
    }

    private int pickRouteIndex(List<List<Integer>> routes, Random rng) {
        List<Integer> eligible = new ArrayList<>();
        for (int i = 0; i < routes.size(); i++) {
            if (routes.get(i) != null && routes.get(i).size() >= 2) {
                eligible.add(i);
            }
        }
        if (eligible.isEmpty()) {
            return -1;
        }
        return eligible.get(rng.nextInt(eligible.size()));
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
}
