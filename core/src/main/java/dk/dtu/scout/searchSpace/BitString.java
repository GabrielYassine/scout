package dk.dtu.scout.searchSpace;

import dk.dtu.scout.dto.Parameter;
import dk.dtu.scout.State;
import dk.dtu.scout.datatypes.StateKeys;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 *
 * @author s235257 & s230632
 */
@Component
@Scope("prototype")
public class BitString implements SearchSpace<boolean[]> {

    private int n = 100;

    public BitString() {}

    @Override
    public void init(State state) {
        state.update(Map.of(StateKeys.DIMENSION, n));
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
        return List.of(new Parameter("n", "Length (n)", "int", n, 1.0, null, null));
    }

    @Override
    public void configure(Map<String, Object> params) {
        int value = ((Number) params.get("n")).intValue();
        if (value <= 0) {
            throw new IllegalArgumentException("Bitstring length must be positive");
        }
        this.n = value;
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