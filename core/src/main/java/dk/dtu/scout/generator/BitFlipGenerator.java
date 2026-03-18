package dk.dtu.scout.generator;

import dk.dtu.scout.ConfigurationContext;
import dk.dtu.scout.Parameter;
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

    public void configure(Map<String, Object> params, ConfigurationContext context) {
        if (params == null) {
            params = Map.of();
        }
        Object flipProbValue = params.get("flipProbability");
        if (flipProbValue instanceof String) {
            String formula = (String) flipProbValue;
            double p = FormulaEvaluator.eval(formula, context.getDimension());
            p = Math.max(0.0, Math.min(1.0, p));
            this.flipProbability = p;
        } else if (flipProbValue instanceof Number) {
            this.flipProbability = ((Number) flipProbValue).doubleValue();
        } else {
            this.flipProbability = 1.0 / context.getDimension();
        }
    }

    @Override
    public List<String> supportedSearchSpaces() { return List.of("bitstring"); }

    @Override
    public boolean[] generate(boolean[] bits, Random rng) {
        int n = bits.length;
        if (n == 0) return bits;
        boolean[] mutated = bits.clone();
        for (int i = 0; i < n; i++) {
            if (rng.nextDouble() < flipProbability) mutated[i] = !mutated[i];
        }
        return mutated;
    }
}