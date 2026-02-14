package dk.dtu.scout.mutation;

import dk.dtu.scout.Parameter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Random;

@Component
public class BitMutation implements Mutation<boolean[]> {

    public enum Mode {
        SINGLE_BIT,          // flip exactly one bit
        INDEPENDENT_PROB     // flip each bit with probability p
    }

    private Mode mode = Mode.SINGLE_BIT;
    private double flipProbability = 0.0;

    public BitMutation() {}

    @Override
    public void configure(Map<String, Object> params) {
        if (params == null) return;
        if (params.containsKey("mode")) {
            this.mode = (Mode) params.get("mode");
        }
        if (params.containsKey("flipProbability")) {
            this.flipProbability = ((Number) params.get("flipProbability")).doubleValue();
        }
    }

    /** Factory: flip exactly one random bit */
    public static BitMutation singleBit() {
        BitMutation mutation = new BitMutation();
        mutation.mode = Mode.SINGLE_BIT;
        mutation.flipProbability = 0.0;
        return mutation;
    }

    /** Factory: flip each bit independently with probability p */
    public static BitMutation withProbability(double p) {
        if (p < 0.0 || p > 1.0) {
            throw new IllegalArgumentException("flipProbability must be in [0,1]");
        }
        BitMutation mutation = new BitMutation();
        mutation.mode = Mode.INDEPENDENT_PROB;
        mutation.flipProbability = p;
        return mutation;
    }

    @Override
    public String id() {
        return mode == Mode.SINGLE_BIT ? "single-bit-flip" : "bit-flip";
    }

    @Override
    public String displayName() {
        return "Bit Flip Mutation";
    }

    @Override
    public String description() {
        return mode == Mode.SINGLE_BIT
            ? "Flips a single randomly chosen bit in a bitstring"
            : "Flips each bit in a bitstring with a certain probability";
    }

    @Override
    public List<Parameter> params() {
        if (mode == Mode.SINGLE_BIT) {
            return List.of();
        }
        return List.of(
            new Parameter("flipProbability", "Flip Probability", "string", "1/n", null, null)
        );
    }

    @Override
    public List<String> supportedSearchSpaces() {
        return List.of("bitstring");
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
