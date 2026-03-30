package dk.dtu.scout.crossover;

import dk.dtu.scout.Parameter;
import dk.dtu.scout.State;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Component
@Scope("prototype")
public class OrderCrossover implements Crossover<int[]> {

    private State state;

    @Override
    public void init(State state) {
        this.state = state;
    }

    @Override
    public String id() {
        return "ox";
    }

    @Override
    public String displayName() {
        return "Order Crossover (OX)";
    }

    @Override
    public String description() {
        return "Copies a segment from the first parent and fills the remaining positions in the order they appear in the second parent.";
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
    }

    @Override
    public int[] crossover(Random rng) {
        Object p1Obj = state.get("selectedParent1");
        Object p2Obj = state.get("selectedParent2");

        if (!(p1Obj instanceof int[] parent1) || !(p2Obj instanceof int[] parent2)) {
            throw new IllegalStateException("OrderCrossover requires 'selectedParent1' and 'selectedParent2' in state");
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
        boolean[] used = new boolean[n];

        // copy middle segment from parent1
        for (int i = cut1; i <= cut2; i++) {
            child[i] = parent1[i];
            used[parent1[i]] = true;
        }

        // fill remaining positions using parent2 order, starting after cut2
        int childPos = (cut2 + 1) % n;
        int parent2Pos = (cut2 + 1) % n;

        for (int filled = 0; filled < n; filled++) {
            int gene = parent2[parent2Pos];
            if (!used[gene]) {
                child[childPos] = gene;
                used[gene] = true;
                childPos = (childPos + 1) % n;
                while (child[childPos] != -1) {
                    childPos = (childPos + 1) % n;
                }
            }
            parent2Pos = (parent2Pos + 1) % n;
        }

        return child;
    }
}