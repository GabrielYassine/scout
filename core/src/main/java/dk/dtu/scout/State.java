package dk.dtu.scout;

import dk.dtu.scout.datatypes.StateKeys;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Shared per-run blackboard for cross-component communication.
 *
 * <p>Values are keyed by strings; prefer {@link StateKeys} for canonical keys.
 */
public class State {

    private final Map<String, Object> variables = Collections.synchronizedMap(new HashMap<>());

    public void update(Map<String, Object> vars) {
        if (vars != null) {
            variables.putAll(vars);
        }
    }

    public Object get(String key) {
        return variables.get(key);
    }
}
