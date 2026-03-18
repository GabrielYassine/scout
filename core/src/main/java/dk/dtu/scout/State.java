package dk.dtu.scout;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
