package dk.dtu.scout;

import java.util.List;
import java.util.Map;

public interface ScoutComponent {
    String id();
    String displayName();
    String description();
    List<Parameter> params();
    default List<String> supportedSearchSpaces() { return List.of(); }
    default Map<String, Object> getStateVariables(State state) { return Map.of(); }
    default void init(State state) {}
}
