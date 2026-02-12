package dk.dtu.scout;

import java.util.List;

public interface Component {
    String id();
    String displayName();
    String description();
    List<Parameter> params();
    default List<String> supportedSearchSpaces() { return List.of(); }
}
