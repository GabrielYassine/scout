package dk.dtu.scout.generator;

import dk.dtu.scout.Parameter;
import dk.dtu.scout.State;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Component
@Scope("prototype")
public class SingleBitFlipGenerator implements Generator<boolean[]> {

    private State state;

    @Override
    public void init(State state) {
        this.state = state;
    }

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
    public boolean[] generate(Random rng) {
        Object baseObj = state.get("offspringBase");

        if (!(baseObj instanceof boolean[] bits)) {
            throw new IllegalStateException("BitFlipGenerator requires 'crossoverChild' or 'selectedParent' in state");
        }
        int n = bits.length;
        if (n == 0) return bits;
        boolean[] mutated = bits.clone();
        int i = rng.nextInt(n);
        mutated[i] = !mutated[i];
        return mutated;
    }
}

