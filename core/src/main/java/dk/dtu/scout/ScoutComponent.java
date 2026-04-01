package dk.dtu.scout;

import java.util.List;
import java.util.Map;

/**
 * Base contract for all pluggable Scout components.
 *
 * <p>Components are self-describing and optionally publish shared-state variables.
 */
public interface ScoutComponent {
    String id();
    String displayName();
    String description();
    List<Parameter> params();
    default List<String> supportedSearchSpaces() { return List.of(); }
    default Map<String, Object> getStateVariables(State state) { return Map.of(); }
    default void init(State state) {}
    default void configure(Map<String, Object> params) {}
}
