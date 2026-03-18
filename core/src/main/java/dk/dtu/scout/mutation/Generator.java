package dk.dtu.scout.mutation;

import dk.dtu.scout.Component;

import java.util.Map;
import java.util.Random;

public interface Generator<S> extends Component {
    S generate(S solution, Random rng);
    default void configure(Map<String, Object> params) {}
}
