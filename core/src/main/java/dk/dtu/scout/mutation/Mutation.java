package dk.dtu.scout.mutation;

import dk.dtu.scout.Component;

import java.util.Map;
import java.util.Random;

public interface Mutation<S> extends Component {
    S mutate(S solution, Random rng);
    default void configure(Map<String, Object> params) {}
}
