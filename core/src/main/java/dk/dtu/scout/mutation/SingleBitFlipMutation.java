package dk.dtu.scout.mutation;

import dk.dtu.scout.Parameter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Component
public class SingleBitFlipMutation implements Mutation<boolean[]> {

    @Override
    public String id() { return "single-bit-flip"; }
    @Override
    public String displayName() { return "Single Bit Flip"; }
    @Override
    public String description() { return "Flips exactly one random bit."; }
    @Override
    public List<Parameter> params() { return List.of(); }
    @Override
    public List<String> supportedSearchSpaces() { return List.of("bitstring"); }

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

