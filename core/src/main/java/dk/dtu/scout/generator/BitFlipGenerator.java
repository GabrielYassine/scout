package dk.dtu.scout.generator;

import dk.dtu.scout.dto.Parameter;
import dk.dtu.scout.State;
import dk.dtu.scout.datatypes.StateKeys;
import dk.dtu.scout.util.FormulaEvaluator;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Random;


/**
 * Generator that creates a new bitstring by mutating an existing offspring base.
 * Each bit is flipped independently with probability p. The probability can be
 * configured either as a number or as a formula using the problem dimension.
 * @author s230632
 */
@Component
@Scope("prototype")
public class BitFlipGenerator implements Generator<boolean[]> {
    private double flipProbability = 0.0;
    private Object flipProbabilityParam;
    private State state;

    @Override
    public void init(State state) {
        this.state = state;
        resolveFlipProbability(state);
    }

    public String id() { return "bit-flip"; }
    @Override
    public String displayName() { return "Bit Flip (p)"; }
    @Override
    public String description() { return "Flips each bit independently with probability p."; }
    @Override
    public List<Parameter> params() {
        return List.of(new Parameter("flipProbability", "Flip Probability", "string", "1/n", null, null, null));
    }

    @Override
    public void configure(Map<String, Object> params) {
        this.flipProbabilityParam = params.get("flipProbability");
    }
    /**
     * Resolves the configured flip probability to a numeric value.
     * Formula values are evaluated using the search space dimension n.
     * The final probability must be finite and between 0 and 1.
     * @param state shared run state containing the dimension
     */
    private void resolveFlipProbability(State state) {
        int dimension = ((Number) state.get(StateKeys.DIMENSION)).intValue();
        Object value = flipProbabilityParam != null ? flipProbabilityParam : "1/n";

        double probability = switch (value) {
            case String formula -> FormulaEvaluator.eval(formula, dimension);
            case Number number -> number.doubleValue();
            default -> throw new IllegalArgumentException("flipProbability must be a number or formula");
        };

        this.flipProbability = Math.max(0.0, Math.min(1.0, probability));
    }

    @Override
    public List<String> supportedSearchSpaces() { return List.of("bitstring"); }
    /**
     * Generates a mutated copy of the offspring base.
     * @param rng random number generator used to decide whether each bit flips
     * @return mutated bitstring
     */
    @Override
    public boolean[] generate(Random rng) {
        Object baseObj = state.get(StateKeys.OFFSPRING_BASE);

        if (!(baseObj instanceof boolean[] bits)) {
            throw new IllegalStateException("BitFlipGenerator requires 'offspringBase' in state");
        }
        int n = bits.length;
        if (n == 0) return bits;
        boolean[] mutated = bits.clone();
        for (int i = 0; i < n; i++) {
            if (rng.nextDouble() < flipProbability) mutated[i] = !mutated[i];
        }
        return mutated;
    }
}