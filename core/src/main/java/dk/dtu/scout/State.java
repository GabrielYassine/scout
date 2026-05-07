package dk.dtu.scout;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralized state management for the Scout framework, allowing components to share and update variables during execution.
 * @author s235257 & s230632
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
