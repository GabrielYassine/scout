package dk.dtu.scout.mutation;

import java.util.Random;

public interface Mutation<S> {
    S mutate(S solution, Random rng);
}
