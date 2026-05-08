package dk.dtu.scout.generator;

import dk.dtu.scout.State;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static dk.dtu.scout.generator.GeneratorTestSupport.FixedIntRandom;
import static dk.dtu.scout.generator.GeneratorTestSupport.sortedFlattened;
import static dk.dtu.scout.generator.GeneratorTestSupport.stateWithRouteBase;
import static dk.dtu.scout.generator.GeneratorTestSupport.stateWithSelectedRouteParent;
import static org.junit.jupiter.api.Assertions.*;

class RouteListRelocateGeneratorTest {

    @Test
    void generate_relocatesWithinSameRoute() {
        RouteListRelocateGenerator generator = new RouteListRelocateGenerator();
        List<List<Integer>> base = List.of(List.of(0, 1, 2));

        generator.init(stateWithRouteBase(base));

        List<List<Integer>> result = generator.generate(new FixedIntRandom(0, 1, 0, 2));

        assertEquals(List.of(List.of(0, 2, 1)), result);
        assertEquals(List.of(0, 1, 2), sortedFlattened(result));
    }

    @Test
    void generate_relocatesAcrossRoutes() {
        RouteListRelocateGenerator generator = new RouteListRelocateGenerator();
        List<List<Integer>> base = List.of(List.of(0, 1), List.of(2));

        generator.init(stateWithRouteBase(base));

        List<List<Integer>> result = generator.generate(new FixedIntRandom(0, 0, 1, 1));

        assertEquals(List.of(List.of(1), List.of(2, 0)), result);
        assertEquals(List.of(0, 1, 2), sortedFlattened(result));
    }

    @Test
    void generate_fallsBackToSelectedParentWhenOffspringBaseMissing() {
        RouteListRelocateGenerator generator = new RouteListRelocateGenerator();
        List<List<Integer>> parent = List.of(List.of(0, 1), List.of(2));

        generator.init(stateWithSelectedRouteParent(parent));

        List<List<Integer>> result = generator.generate(new FixedIntRandom(0, 0, 1, 0));

        assertEquals(List.of(0, 1, 2), sortedFlattened(result));
    }

    @Test
    void generate_returnsCopiedRoutesWhenAllRoutesAreEmpty() {
        RouteListRelocateGenerator generator = new RouteListRelocateGenerator();
        List<List<Integer>> base = List.of(List.of(), List.of());

        generator.init(stateWithRouteBase(base));

        List<List<Integer>> result = generator.generate(new Random(1234L));

        assertEquals(base, result);
        assertNotSame(base, result);
        assertNotSame(base.getFirst(), result.getFirst());
    }

    @Test
    void generate_rejectsMissingBaseAndParent() {
        RouteListRelocateGenerator generator = new RouteListRelocateGenerator();

        generator.init(new State());

        assertThrows(IllegalStateException.class, () -> generator.generate(new Random(1234L)));
    }

    @Test
    void generate_rejectsNonListRoute() {
        RouteListRelocateGenerator generator = new RouteListRelocateGenerator();

        generator.init(stateWithRouteBase(List.of("not-a-route")));

        assertThrows(IllegalArgumentException.class, () -> generator.generate(new Random(1234L)));
    }

    @Test
    void generate_rejectsNonNumericCustomerIndex() {
        RouteListRelocateGenerator generator = new RouteListRelocateGenerator();

        generator.init(stateWithRouteBase(List.of(List.of("bad-customer"))));

        assertThrows(IllegalArgumentException.class, () -> generator.generate(new Random(1234L)));
    }

    @Test
    void metadata_isStable() {
        RouteListRelocateGenerator generator = new RouteListRelocateGenerator();

        assertEquals("route-list-relocate", generator.id());
        assertEquals("Route List Relocate", generator.displayName());
        assertFalse(generator.description().isBlank());
        assertTrue(generator.params().isEmpty());
        assertEquals(List.of("route-list"), generator.supportedSearchSpaces());
    }
}