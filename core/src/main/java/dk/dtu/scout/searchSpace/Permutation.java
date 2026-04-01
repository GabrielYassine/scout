package dk.dtu.scout.searchSpace;

import dk.dtu.scout.Parameter;
import dk.dtu.scout.State;
import dk.dtu.scout.StateKeys;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Represents a permutation search space.
 * Solutions are represented as integer arrays containing permutations of {0, 1, ..., n-1}.
 */

@Component
@Scope("prototype")
public class Permutation implements SearchSpace<int[]> {

    private int n = 10;

    public Permutation() {}

    @Override
    public void init(State state) {
        if (state != null) {
            state.update(Map.of(StateKeys.DIMENSION, n));
        }
    }

    @Override public int dimension() {
        return n;
    }

    @Override public String id() {
        return "permutation";
    }

    @Override public String displayName() {
        return "Permutation";
    }

    @Override public String description() {
        return "Permutation of integers {0, 1, ..., n-1} for combinatorial optimization problems,e.g. city visiting order in TSP";
    }

    @Override public List<Parameter> params() {
        return List.of(new Parameter("n", "Length (n)", "int", n, 1.0, null));
    }

    @Override
    public void configure(Map<String, Object> params) {
        if (params == null) return;
        if (params.containsKey("n")) {
            int value = ((Number) params.get("n")).intValue();
            if (value <= 0) {
                throw new IllegalArgumentException("Permutation length must be positive");
            }
            this.n = value;
        }
    }

    @Override
    public int[] randomSolution(Random rng) {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            list.add(i);
        }
        Collections.shuffle(list, rng);

        int[] perm = new int[n];
        for (int i = 0; i < n; i++) {
            perm[i] = list.get(i);
        }
        return perm;
    }
}
