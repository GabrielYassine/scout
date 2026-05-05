package dk.dtu.scout.crossover;

import dk.dtu.scout.dto.Parameter;
import dk.dtu.scout.State;
import dk.dtu.scout.datatypes.StateKeys;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Scope("prototype")
public class KPointCrossover implements Crossover<boolean[]> {
    private State state;

    private int k = 1;

    public void init(State state) {
        this.state = state;
    }

    @Override
    public String id() {
        return "k-point";
    }

    @Override
    public String displayName() {
        return "K-Point Crossover";
    }

    @Override
    public String description() {
        return "Chooses k crossing points at random and performs a k-point crossover.";
    }

    @Override
    public List<Parameter> params() {
        return List.of(
                new Parameter("k", "Number of crossover points", "int", k, 1.0, null, null)
        );
    }

    @Override
    public List<String> supportedSearchSpaces() {
        return List.of("bitstring");
    }

    @Override
    public void configure(Map<String, Object> params) {
        if (params == null) return;
        if (params.containsKey("k")) {
            int value = ((Number) params.get("k")).intValue();
            if (value <= 0) {
                throw new IllegalArgumentException("k must be positive");
            }
            this.k = value;
        }
    }

    @Override
    public boolean[] crossover(Random rng) {
        Object p1Obj = state.get(StateKeys.SELECTED_PARENT_1);
        Object p2Obj = state.get(StateKeys.SELECTED_PARENT_2);

        if (!(p1Obj instanceof boolean[] parent1) || !(p2Obj instanceof boolean[] parent2)) {
            throw new IllegalStateException("KPointBitstringCrossover requires 'selectedParent1' and 'selectedParent2' in state");
        }

        if (parent1.length != parent2.length) {
            throw new IllegalArgumentException("Parents must have same length");
        }

        int n = parent1.length;
        if (n == 0) return new boolean[0];
        if (n == 1) return new boolean[] { rng.nextBoolean() ? parent1[0] : parent2[0] };

        if (k > n - 1) {
            throw new IllegalArgumentException("k must be at most n-1");
        }

        List<Integer> cuts = randomDistinctCuts(k, n, rng);

        boolean[] child = new boolean[n];
        boolean useFirstParent = true;

        int cutIndex = 0;
        int nextCut = cuts.get(cutIndex);

        for (int i = 0; i < n; i++) {
            if (i == nextCut) {
                useFirstParent = !useFirstParent;
                cutIndex++;
                nextCut = cutIndex < cuts.size() ? cuts.get(cutIndex) : -1;
            }
            child[i] = useFirstParent ? parent1[i] : parent2[i];
        }

        return child;
    }

    private List<Integer> randomDistinctCuts(int k, int n, Random rng) {
        Set<Integer> chosen = new HashSet<>();
        while (chosen.size() < k) {
            chosen.add(1 + rng.nextInt(n - 1));
        }
        List<Integer> cuts = new ArrayList<>(chosen);
        Collections.sort(cuts);
        return cuts;
    }
}