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
 *
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