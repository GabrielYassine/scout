package dk.dtu.scout;

import dk.dtu.scout.dto.Parameter;

import java.util.List;
import java.util.Map;

/**
 *
 * @author s235257 & s230632
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
