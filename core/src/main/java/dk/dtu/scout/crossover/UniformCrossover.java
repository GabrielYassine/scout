package dk.dtu.scout.crossover;

import dk.dtu.scout.dto.Parameter;
import dk.dtu.scout.State;
import dk.dtu.scout.datatypes.StateKeys;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Random;

@Component
@Scope("prototype")
public class UniformCrossover implements Crossover<boolean[]> {
    private State state;

    public void init(State state) {
        this.state = state;
    }
    @Override
    public String id() {
        return "uniform";
    }

    @Override
    public String displayName() {
        return "Uniform Crossover";
    }

    @Override
    public String description() {
        return "Each bit is taken with probability 1/2 from the first parent and with probability 1/2 from the second parent.";
    }

    @Override
    public List<Parameter> params() {
        return List.of();
    }

    @Override
    public List<String> supportedSearchSpaces() {
        return List.of("bitstring");
    }

    @Override
    public void configure(Map<String, Object> params) {
        // no parameters
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
        boolean[] child = new boolean[n];

        for (int i = 0; i < n; i++) {
            child[i] = rng.nextBoolean() ? parent1[i] : parent2[i];
        }
        return child;
    }
}