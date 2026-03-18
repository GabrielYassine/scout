package dk.dtu.scout.searchSpace;

import dk.dtu.scout.Parameter;
import dk.dtu.scout.State;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Represents a binary string (bitstring) search space.
 * Solutions are represented as boolean arrays.
 */

@Component
public class BitString implements SearchSpace<boolean[]> {

    private int n = 100;

    public BitString() {}

    @Override
    public void init(State state) {
        if (state != null) {
            state.update(Map.of("dimension", n));
        }
    }

    @Override
    public int dimension() {
        return n;
    }
    @Override
    public String id() {
        return "bitstring";
    }
    @Override
    public String displayName() {
        return "BitString";
    }
    @Override
    public String description() {
        return "Binary string representation for boolean optimization problems";
    }

    @Override
    public List<Parameter> params() {
        return List.of(new Parameter("n", "Length (n)", "int", n, 1.0, null));
    }

    @Override
    public void configure(Map<String, Object> params) {
        if (params == null) return;
        if (params.containsKey("n")) {
            int value = ((Number) params.get("n")).intValue();
            if (value <= 0) {
                throw new IllegalArgumentException("Bitstring length must be positive");
            }
            this.n = value;
        }
    }

    @Override
    public boolean[] randomSolution(Random rng) {
        boolean[] bits = new boolean[n];
        for (int i = 0; i < n; i++) {
            bits[i] = rng.nextBoolean();
        }
        return bits;
    }
}