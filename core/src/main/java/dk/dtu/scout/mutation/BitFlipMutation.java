package dk.dtu.scout.mutation;

import java.util.Random;

public class BitFlipMutation implements Mutation<boolean[]> {
    @Override
    public boolean[] mutate(boolean[] bits, Random rng) { // Take probability and bit flip amount as parameters
        int n = bits.length; // so we dont compute length multiple times
        if (n == 0) return bits;

        double probability = 1.0 / n;
        boolean[] mutated = bits.clone();

        for (int i = 0; i < n; i++) {
            if (rng.nextDouble() < probability) {
                mutated[i] = !mutated[i];
            }
        }
        return mutated;
    }
}
