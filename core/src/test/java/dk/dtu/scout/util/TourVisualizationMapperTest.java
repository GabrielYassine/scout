package dk.dtu.scout.util;

import dk.dtu.scout.datatypes.TSPInstance;
import dk.dtu.scout.datatypes.VRPInstance;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TourVisualizationMapperTest {

    @Test
    void buildTspCities_mapsCoordinatesToCityObjects() {
        TSPInstance instance = tspInstance();

        List<Map<String, Object>> cities = TourVisualizationMapper.buildTspCities(instance);

        assertEquals(3, cities.size());

        assertEquals(0.0, (double) cities.get(0).get("x"), 1e-9);
        assertEquals(0.0, (double) cities.get(0).get("y"), 1e-9);

        assertEquals(3.0, (double) cities.get(1).get("x"), 1e-9);
        assertEquals(4.0, (double) cities.get(1).get("y"), 1e-9);

        assertEquals(6.0, (double) cities.get(2).get("x"), 1e-9);
        assertEquals(0.0, (double) cities.get(2).get("y"), 1e-9);

        assertFalse(cities.get(0).containsKey("isDepot"));
    }

    @Test
    void buildVrpCities_addsDepotFirstAndThenCustomers() {
        VRPInstance instance = vrpInstance();

        List<Map<String, Object>> cities = TourVisualizationMapper.buildVrpCities(instance);

        assertEquals(3, cities.size());

        Map<String, Object> depot = cities.getFirst();
        assertEquals(0.0, (double) depot.get("x"), 1e-9);
        assertEquals(0.0, (double) depot.get("y"), 1e-9);
        assertEquals(true, depot.get("isDepot"));

        assertEquals(3.0, (double) cities.get(1).get("x"), 1e-9);
        assertEquals(4.0, (double) cities.get(1).get("y"), 1e-9);
        assertFalse(cities.get(1).containsKey("isDepot"));

        assertEquals(6.0, (double) cities.get(2).get("x"), 1e-9);
        assertEquals(0.0, (double) cities.get(2).get("y"), 1e-9);
        assertFalse(cities.get(2).containsKey("isDepot"));
    }

    @Test
    void wrapTour_convertsArrayToSingleRoute() {
        List<List<Integer>> wrapped = TourVisualizationMapper.wrapTour(new int[] {2, 0, 1});

        assertEquals(List.of(List.of(2, 0, 1)), wrapped);
    }

    @Test
    void wrapTour_returnsEmptyListForNullTour() {
        assertEquals(List.of(), TourVisualizationMapper.wrapTour(null));
    }

    @Test
    void wrapTour_returnsEmptyListForEmptyTour() {
        assertEquals(List.of(), TourVisualizationMapper.wrapTour(new int[] {}));
    }

    @Test
    void shiftRoutesForDepot_addsOneToCustomerIndices() {
        List<List<Integer>> shifted = TourVisualizationMapper.shiftRoutesForDepot(List.of(
            List.of(0, 1),
            List.of(2)
        ));

        assertEquals(List.of(List.of(1, 2), List.of(3)), shifted);
    }

    @Test
    void shiftRoutesForDepot_keepsEmptyRoutesAsEmptyRoutes() {
        List<List<Integer>> shifted = TourVisualizationMapper.shiftRoutesForDepot(List.of(
            List.of(),
            List.of(0)
        ));

        assertEquals(List.of(List.of(), List.of(1)), shifted);
    }

    @Test
    void shiftRoutesForDepot_skipsNullCustomerValuesInsideRoute() {
        List<Integer> routeWithNull = new ArrayList<>();
        routeWithNull.add(0);
        routeWithNull.add(null);
        routeWithNull.add(2);

        List<List<Integer>> shifted = TourVisualizationMapper.shiftRoutesForDepot(List.of(routeWithNull));

        assertEquals(List.of(List.of(1, 3)), shifted);
    }

    @Test
    void shiftRoutesForDepot_keepsNullRouteAsEmptyRoute() {
        List<List<Integer>> routes = new ArrayList<>();
        routes.add(null);
        routes.add(List.of(1));

        List<List<Integer>> shifted = TourVisualizationMapper.shiftRoutesForDepot(routes);

        assertEquals(List.of(List.of(), List.of(2)), shifted);
    }

    private static TSPInstance tspInstance() {
        return new TSPInstance(
            "triangle",
            "test instance",
            3,
            new double[][] {
                {0.0, 0.0},
                {3.0, 4.0},
                {6.0, 0.0}
            }
        );
    }

    private static VRPInstance vrpInstance() {
        return new VRPInstance(
            "vrp-test",
            "test instance",
            new double[] {0.0, 0.0},
            new double[][] {
                {3.0, 4.0},
                {6.0, 0.0}
            },
            new double[] {1.0, 1.0},
            10.0,
            2
        );
    }
}