package dk.dtu.scout.crossover;

import dk.dtu.scout.dto.EvaluatedSolution;
import dk.dtu.scout.dto.Parameter;
import dk.dtu.scout.State;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Random;

@Component
@Scope("prototype")
public class GenepoolCrossover implements Crossover<boolean[]> {

    private State state;

    @Override
    public void init(State state) {
        this.state = state;
    }

    @Override
    public String id() {
        return "genepool";
    }

    @Override
    public String displayName() {
        return "Genepool Crossover";
    }

    @Override
    public String description() {
        return "Builds offspring from a whole parent population by setting each bit to 1 with probability k/n, where k is the number of ones at that position.";
    }

    @Override
    public List<Parameter> params() {
        return List.of();
    }

    @Override
    public List<String> supportedSearchSpaces() {
        return List.of("bitstring");
    }

    @Override
    public void configure(Map<String, Object> params) {
        // no parameters for now
    }

    @Override
    public boolean[] crossover(Random rng) {

        Object parentsObj = state.get("parentsEvaluated");
        if (!(parentsObj instanceof List<?> rawParents) || rawParents.isEmpty()) {
            throw new IllegalStateException("GenepoolCrossoverGenerator requires non-empty 'parentsEvaluated' in state");
        }

        int n = rawParents.size();

        Object firstObj = rawParents.get(0);
        if (!(firstObj instanceof EvaluatedSolution<?> firstEval) || !(firstEval.value() instanceof boolean[] firstBits)) {
            throw new IllegalStateException("parentsEvaluated must contain EvaluatedSolution<boolean[]>");
        }

        int dimension = firstBits.length;
        boolean[] child = new boolean[dimension];

        for (int i = 0; i < dimension; i++) {
            int ones = 0;

            for (Object obj : rawParents) {
                if (!(obj instanceof EvaluatedSolution<?> eval) || !(eval.value() instanceof boolean[] bits)) {
                    throw new IllegalStateException("parentsEvaluated must contain EvaluatedSolution<boolean[]>");
                }
                if (bits.length != dimension) {
                    throw new IllegalStateException("All parent bitstrings must have the same length");
                }
                if (bits[i]) {
                    ones++;
                }
            }
            double p = (double) ones / n;
            child[i] = rng.nextDouble() < p;
        }
        return child;
    }
}