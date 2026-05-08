package dk.dtu.scout.generator;

import dk.dtu.scout.dto.Parameter;
import dk.dtu.scout.State;
import dk.dtu.scout.datatypes.StateKeys;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generator that relocates a single customer from one position to another within the same route or across routes.
 * @author s235257
 */
@Component
@Scope("prototype")
public class RouteListRelocateGenerator implements Generator<List<List<Integer>>> {

    private State state;

    @Override
    public void init(State state) {
        this.state = state;
    }

    @Override
    public String id() {
        return "route-list-relocate";
    }

    @Override
    public String displayName() {
        return "Route List Relocate";
    }

    @Override
    public String description() {
        return "Relocates one customer to another position within the same route or across routes";
    }

    @Override
    public List<Parameter> params() {
        return List.of();
    }

    @Override
    public List<String> supportedSearchSpaces() {
        return List.of("route-list");
    }
    /**
     * Generates a new solution by relocating a single customer from one route to another.
     * @param rng random number generator for stochastic choices
     * @return a new list of routes with one customer relocated
     */
    @Override
    public List<List<Integer>> generate(Random rng) {
        Object baseObj = state.get(StateKeys.OFFSPRING_BASE);
        if (baseObj == null) {
            baseObj = state.get(StateKeys.SELECTED_PARENT_1);
        }

        if (!(baseObj instanceof List<?> baseRoutes)) {
            throw new IllegalStateException(
                "RouteListRelocateGenerator requires 'offspringBase' or 'selectedParent1' in state"
            );
        }

        List<List<Integer>> routes = copyRoutes(baseRoutes);
        int sourceRouteIndex = pickNonEmptyRouteIndex(routes, rng);
        if (sourceRouteIndex < 0) {
            return routes;
        }

        List<Integer> sourceRoute = routes.get(sourceRouteIndex);
        int sourcePos = rng.nextInt(sourceRoute.size());
        int customer = sourceRoute.remove(sourcePos);

        int targetRouteIndex = rng.nextInt(routes.size());
        List<Integer> targetRoute = routes.get(targetRouteIndex);

        int insertPos = rng.nextInt(targetRoute.size() + 1);
        targetRoute.add(insertPos, customer);

        return routes;
    }
    /**
     * Randomly selects an index of a non-empty route from the list of routes.
     * @param routes list of routes to select from
     * @param rng random number generator for selection
     * @return index of a non-empty route, or -1 if all routes are empty
     */
    private int pickNonEmptyRouteIndex(List<List<Integer>> routes, Random rng) {
        List<Integer> eligible = new ArrayList<>();

        for (int i = 0; i < routes.size(); i++) {
            if (!routes.get(i).isEmpty()) {
                eligible.add(i);
            }
        }

        if (eligible.isEmpty()) {
            return -1;
        }

        return eligible.get(rng.nextInt(eligible.size()));
    }

    /**
     * Creates a deep copy of the list of routes, ensuring that each route is a new list of integers.
     * @param baseRoutes the original list of routes to copy, expected to be a List<List<Integer>>
     * @return a new List<List<Integer>> where each route is a separate list containing the same customer indices as the original
     */
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