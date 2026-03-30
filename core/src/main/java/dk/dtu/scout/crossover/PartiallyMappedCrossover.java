package dk.dtu.scout.crossover;

import dk.dtu.scout.Parameter;
import dk.dtu.scout.State;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Component
@Scope("prototype")
public class PartiallyMappedCrossover implements Crossover<int[]> {

    private State state;

    @Override
    public void init(State state) {
        this.state = state;
    }

    @Override
    public String id() {
        return "pmx";
    }

    @Override
    public String displayName() {
        return "Partially Mapped Crossover (PMX)";
    }

    @Override
    public String description() {
        return "Copies a segment from the first parent and uses a mapping to place the remaining values from the second parent without duplicates.";
    }

    @Override
    public List<Parameter> params() {
        return List.of();
    }

    @Override
    public List<String> supportedSearchSpaces() {
        return List.of("permutation");
    }

    @Override
    public void configure(Map<String, Object> params) {
        // no params
    }

    @Override
    public int[] crossover(Random rng) {
        Object p1Obj = state.get("selectedParent1");
        Object p2Obj = state.get("selectedParent2");

        if (!(p1Obj instanceof int[] parent1) || !(p2Obj instanceof int[] parent2)) {
            throw new IllegalStateException("PartiallyMappedCrossover requires 'selectedParent1' and 'selectedParent2' in state");
        }

        if (parent1.length != parent2.length) {
            throw new IllegalArgumentException("Parents must have same length");
        }

        int n = parent1.length;
        if (n == 0) return new int[0];
        if (n == 1) return parent1.clone();

        int cut1 = rng.nextInt(n);
        int cut2 = rng.nextInt(n);
        while (cut2 == cut1) {
            cut2 = rng.nextInt(n);
        }
        if (cut1 > cut2) {
            int tmp = cut1;
            cut1 = cut2;
            cut2 = tmp;
        }

        int[] child = new int[n];
        Arrays.fill(child, -1);

        // copy segment from parent1
        for (int i = cut1; i <= cut2; i++) {
            child[i] = parent1[i];
        }

        // mapping: parent2 segment value -> parent1 segment value
        Map<Integer, Integer> mapping = new HashMap<>();
        for (int i = cut1; i <= cut2; i++) {
            mapping.put(parent2[i], parent1[i]);
        }

        // fill outside segment
        for (int i = 0; i < n; i++) {
            if (i >= cut1 && i <= cut2) {
                continue;
            }

            int candidate = parent2[i];

            while (contains(child, candidate, cut1, cut2)) {
                candidate = mapping.get(candidate);
            }

            child[i] = candidate;
        }

        return child;
    }

    private boolean contains(int[] child, int value, int from, int to) {
        for (int i = from; i <= to; i++) {
            if (child[i] == value) {
                return true;
            }
        }
        return false;
    }
}