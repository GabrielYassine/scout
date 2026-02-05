package dk.dtu.scout.searchSpace;

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
    public boolean[] randomSolution(Random rng) {
        boolean[] bits = new boolean[n];
        for (int i = 0; i < n; i++) {
            bits[i] = rng.nextBoolean();
        }
        return bits;
    }
}