package dk.dtu.scout.searchSpace;

import dk.dtu.scout.Parameter;

import java.util.List;
import java.util.Random;

/**
 * Represents a binary string (bitstring) search space.
 * Solutions are represented as boolean arrays.
 */
public class BitString implements SearchSpace<boolean[]> {

    private final int n;

    public BitString(int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("Bitstring length must be positive");
        }
        this.n = n;
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
        return List.of(
            new Parameter("n", "Length (n)", "int", 100, 1.0, null)
        );
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