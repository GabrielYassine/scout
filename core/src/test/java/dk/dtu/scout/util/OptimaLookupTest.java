package dk.dtu.scout.util;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OptimaLookupTest {

    @Test
    void normalizeInstanceKey_handlesNullWhitespaceCaseAndKnownExtensions() {
        assertEquals("", OptimaLookup.normalizeInstanceKey(null));
        assertEquals("eil51", OptimaLookup.normalizeInstanceKey(" EIL51.TSP "));
        assertEquals("a-n32-k5", OptimaLookup.normalizeInstanceKey(" A-N32-K5.vrp "));
        assertEquals("plain-name", OptimaLookup.normalizeInstanceKey(" Plain-Name "));
    }

    @Test
    void normalizeInstanceKey_onlyRemovesFinalTspOrVrpExtension() {
        assertEquals("file.name", OptimaLookup.normalizeInstanceKey("file.name.tsp"));
        assertEquals("file.name", OptimaLookup.normalizeInstanceKey("file.name.vrp"));
        assertEquals("file.tsp.txt", OptimaLookup.normalizeInstanceKey("file.tsp.txt"));
    }

    @Test
    void resolve_usesNormalizedInstanceName() {
        Map<String, Double> optima = Map.of("eil51", 426.0, "a-n32-k5", 784.0);

        assertEquals(426.0, OptimaLookup.resolve(optima, "EIL51.tsp"));
        assertEquals(784.0, OptimaLookup.resolve(optima, " A-N32-K5.VRP "));
        assertNull(OptimaLookup.resolve(optima, "unknown.tsp"));
        assertNull(OptimaLookup.resolve(optima, null));
    }

    @Test
    void loadDoubleMap_returnsEmptyMapForMissingResource() {
        Map<String, Double> result = OptimaLookup.loadDoubleMap("missing-resource-does-not-exist.properties");
        assertTrue(result.isEmpty());
    }

    @Test
    void loadDoubleMap_loadsExistingResourceNormalizesKeysAndSkipsInvalidValues() {
        Map<String, Double> result = OptimaLookup.loadDoubleMap("optima/test-optima.properties");

        assertEquals(426.0, result.get("eil51"));
        assertEquals(784.0, result.get("a-n32-k5"));
        assertFalse(result.containsKey("invalid"));
    }
}