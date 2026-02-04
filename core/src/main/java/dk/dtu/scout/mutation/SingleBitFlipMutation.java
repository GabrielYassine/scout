package dk.dtu.scout.mutation;

import java.util.Random;

/**
 * flip exactly one random bit.
 */
public class SingleBitFlipMutation implements Mutation<boolean[]> {

    @Override
    public boolean[] mutate(boolean[] bits, Random rng) {
        int n = bits.length;
        if (n == 0) return bits;

        boolean[] mutated = bits.clone();
        int i = rng.nextInt(n);
        mutated[i] = !mutated[i];
        return mutated;
    }
}
