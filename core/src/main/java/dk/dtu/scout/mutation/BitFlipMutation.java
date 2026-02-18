package dk.dtu.scout.mutation;

import dk.dtu.scout.Parameter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Random;

@Component
public class BitFlipMutation implements Mutation<boolean[]> {
    private double flipProbability = 0.0;
    @Override
    public String id() { return "bit-flip"; }
    @Override
    public String displayName() { return "Bit Flip (p)"; }
    @Override
    public String description() { return "Flips each bit independently with probability p."; }
    @Override
    public List<Parameter> params() {
        return List.of(new Parameter("flipProbability", "Flip Probability", "string", "1/n", null, null));
    }
    @Override
    public void configure(Map<String, Object> params) {
        if (params == null) return;
        if (params.containsKey("flipProbability")) {
            this.flipProbability = ((Number) params.get("flipProbability")).doubleValue();
        }
    }
    @Override
    public List<String> supportedSearchSpaces() { return List.of("bitstring"); }

    @Override
    public boolean[] mutate(boolean[] bits, Random rng) {
        int n = bits.length;
        if (n == 0) return bits;
        boolean[] mutated = bits.clone();
        for (int i = 0; i < n; i++) {
            if (rng.nextDouble() < flipProbability) mutated[i] = !mutated[i];
        }
        return mutated;
    }
}
