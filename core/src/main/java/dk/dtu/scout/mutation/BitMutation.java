package dk.dtu.scout.mutation;

import java.util.Random;

public class BitMutation implements Mutation<boolean[]> {

    public enum Mode {
        SINGLE_BIT,          // flip exactly one bit
        INDEPENDENT_PROB     // flip each bit with probability p
    }

    private final Mode mode;
    private final double flipProbability;

    private BitMutation(Mode mode, double flipProbability) {
        this.mode = mode;
        this.flipProbability = flipProbability;
    }

    /** Factory: flip exactly one random bit */
    public static BitMutation singleBit() {
        return new BitMutation(Mode.SINGLE_BIT, 0.0);
    }

    /** Factory: flip each bit independently with probability p */
    public static BitMutation withProbability(double p) {
        if (p < 0.0 || p > 1.0) {
            throw new IllegalArgumentException("flipProbability must be in [0,1]");
        }
        return new BitMutation(Mode.INDEPENDENT_PROB, p);
    }

    @Override
    public boolean[] mutate(boolean[] bits, Random rng) {
        int n = bits.length;
        if (n == 0) return bits;

        boolean[] mutated = bits.clone();

        switch (mode) {
            case SINGLE_BIT -> {
                int i = rng.nextInt(n);
                mutated[i] = !mutated[i];
            }
            case INDEPENDENT_PROB -> {
                for (int i = 0; i < n; i++) {
                    if (rng.nextDouble() < flipProbability) {
                        mutated[i] = !mutated[i];
                    }
                }
            }
        }

        return mutated;
    }
}
