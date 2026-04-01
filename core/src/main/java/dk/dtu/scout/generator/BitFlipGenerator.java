package dk.dtu.scout.generator;

import dk.dtu.scout.Parameter;
import dk.dtu.scout.State;
import dk.dtu.scout.StateKeys;
import dk.dtu.scout.util.FormulaEvaluator;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Random;

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
        return List.of(new Parameter("flipProbability", "Flip Probability", "string", "1/n", null, null));
    }

    @Override
    public void configure(Map<String, Object> params) {
        if (params == null) return;
        if (params.containsKey("flipProbability")) {
            this.flipProbabilityParam = params.get("flipProbability");
        }
    }

    private void resolveFlipProbability(State state) {
        int dimension = 0;
        if (state != null) {
            Object dimObj = state.get(StateKeys.DIMENSION);
            if (dimObj instanceof Number n) {
                dimension = n.intValue();
            }
        }

        Object value = flipProbabilityParam;
        double p;
        if (value instanceof String formula) {
            if (dimension > 0) {
                p = FormulaEvaluator.eval(formula, dimension);
            } else {
                p = 0.0;
            }
        } else if (value instanceof Number n) {
            p = n.doubleValue();
        } else {
            p = dimension > 0 ? (1.0 / dimension) : 0.0;
        }

        p = Math.max(0.0, Math.min(1.0, p));
        this.flipProbability = p;
    }

    @Override
    public List<String> supportedSearchSpaces() { return List.of("bitstring"); }

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