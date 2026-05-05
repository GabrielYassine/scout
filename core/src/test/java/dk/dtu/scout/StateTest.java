package dk.dtu.scout;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StateTest {

    @Test
    void update_addsVariables() {
        State state = new State();

        state.update(Map.of("a", 1, "b", "value"));

        assertEquals(1, state.get("a"));
        assertEquals("value", state.get("b"));
    }

    @Test
    void update_overwritesExistingVariable() {
        State state = new State();

        state.update(Map.of("key", "old"));
        state.update(Map.of("key", "new"));

        assertEquals("new", state.get("key"));
    }

    @Test
    void update_ignoresNullMap() {
        State state = new State();

        state.update(null);

        assertNull(state.get("missing"));
    }

    @Test
    void get_returnsNullForMissingKey() {
        State state = new State();

        assertNull(state.get("missing"));
    }
}