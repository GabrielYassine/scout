package dk.dtu.scout.searchspace;

import dk.dtu.scout.State;
import dk.dtu.scout.datatypes.StateKeys;
import dk.dtu.scout.searchSpace.RouteList;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class RouteListTest {

    @Test
    void configure_setsDimensionAndRouteCount() {
        RouteList searchSpace = new RouteList();

        searchSpace.configure(Map.of("n", 6, "routeCount", 3));

        assertEquals(6, searchSpace.dimension());
    }

    @Test
    void configure_rejectsNonPositiveDimension() {
        RouteList searchSpace = new RouteList();

        assertThrows(IllegalArgumentException.class, () -> searchSpace.configure(Map.of("n", 0)));
        assertThrows(IllegalArgumentException.class, () -> searchSpace.configure(Map.of("n", -1)));
    }

    @Test
    void configure_rejectsNonPositiveRouteCount() {
        RouteList searchSpace = new RouteList();

        assertThrows(IllegalArgumentException.class, () -> searchSpace.configure(Map.of("n", 5, "routeCount", 0)));

        assertThrows(IllegalArgumentException.class, () -> searchSpace.configure(Map.of("n", 5, "routeCount", -1)));
    }

    @Test
    void randomSolution_withOneRouteReturnsAllCustomersInSingleRoute() {
        RouteList searchSpace = new RouteList();
        searchSpace.configure(Map.of("n", 5, "routeCount", 1));

        List<List<Integer>> solution = searchSpace.randomSolution(new Random(1234L));

        assertEquals(1, solution.size());
        assertEquals(List.of(0, 1, 2, 3, 4), sortedFlattened(solution));
    }

    @Test
    void randomSolution_withMultipleRoutesContainsEveryCustomerExactlyOnce() {
        RouteList searchSpace = new RouteList();
        searchSpace.configure(Map.of("n", 6, "routeCount", 3));

        List<List<Integer>> solution = searchSpace.randomSolution(new Random(1234L));

        assertEquals(3, solution.size());
        assertEquals(List.of(0, 1, 2, 3, 4, 5), sortedFlattened(solution));
        assertTrue(solution.stream().allMatch(route -> !route.isEmpty()));
    }

    @Test
    void randomSolution_capsRouteCountAtNumberOfCustomers() {
        RouteList searchSpace = new RouteList();
        searchSpace.configure(Map.of("n", 3, "routeCount", 10));

        List<List<Integer>> solution = searchSpace.randomSolution(new Random(1234L));

        assertEquals(3, solution.size());
        assertEquals(List.of(0, 1, 2), sortedFlattened(solution));
    }

    @Test
    void randomSolution_isDeterministicForSameSeed() {
        RouteList searchSpace = new RouteList();
        searchSpace.configure(Map.of("n", 6, "routeCount", 3));

        List<List<Integer>> first = searchSpace.randomSolution(new Random(1234L));
        List<List<Integer>> second = searchSpace.randomSolution(new Random(1234L));

        assertEquals(first, second);
    }

    @Test
    void init_writesDimensionToState() {
        RouteList searchSpace = new RouteList();
        searchSpace.configure(Map.of("n", 7));
        State state = new State();

        searchSpace.init(state);

        assertEquals(7, state.get(StateKeys.DIMENSION));
    }

    @Test
    void metadata_isStable() {
        RouteList searchSpace = new RouteList();

        assertEquals("route-list", searchSpace.id());
        assertEquals("Route List", searchSpace.displayName());
        assertFalse(searchSpace.description().isBlank());
        assertEquals(2, searchSpace.params().size());
    }

    private static List<Integer> sortedFlattened(List<List<Integer>> routes) {
        List<Integer> customers = new ArrayList<>();

        for (List<Integer> route : routes) {
            customers.addAll(route);
        }

        Collections.sort(customers);
        return customers;
    }
}