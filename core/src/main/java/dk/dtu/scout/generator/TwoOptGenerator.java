package dk.dtu.scout.generator;

import dk.dtu.scout.dto.Parameter;
import dk.dtu.scout.State;
import dk.dtu.scout.datatypes.StateKeys;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

/**
 * Generator that creates a new permutation by applying a random 2-opt mutation.
 * The generator reverses a randomly selected segment of the permutation.
 * @author s230632 & s235257
 */
@Component
@Scope("prototype")
public class TwoOptGenerator implements Generator<int[]> {

    private State state;

    @Override
    public void init(State state) {
        this.state = state;
    }

    @Override
    public String id() {
        return "2opt";
    }

    @Override
    public String displayName() {
        return "2-Opt Mutation";
    }

    @Override
    public String description() {
        return "Reverse a random segment of the permutation (2-opt move)";
    }

    @Override
    public List<Parameter> params() {
        return List.of();
    }

    @Override
    public List<String> supportedSearchSpaces() {
        return List.of("permutation");
    }
    /**
     * Generates a mutated permutation by reversing a random segment.
     * @param rng random number generator used to choose the segment endpoints
     * @return mutated permutation
     */
    @Override
    public int[] generate(Random rng) {
        Object baseObj = state.get(StateKeys.OFFSPRING_BASE);
        if (baseObj == null) {
            baseObj = state.get(StateKeys.SELECTED_PARENT_1);
        }
        if (!(baseObj instanceof  int[] solution )) {
            throw new IllegalStateException("TwoOptGenerator requires 'crossoverChild' or 'selectedParent' in state");
        }
        int[] mutated = solution.clone();

        if (mutated.length < 2) {
            return mutated;
        }

        int pos1 = rng.nextInt(mutated.length);
        int pos2 = rng.nextInt(mutated.length);


        if (pos1 > pos2) {
            int temp = pos1;
            pos1 = pos2;
            pos2 = temp;
        }

        if (pos1 == pos2) {
            pos2 = (pos2 + 1) % mutated.length;
            if (pos1 > pos2) {
                int temp = pos1;
                pos1 = pos2;
                pos2 = temp;
            }
        }

        while (pos1 < pos2) {
            int temp = mutated[pos1];
            mutated[pos1] = mutated[pos2];
            mutated[pos2] = temp;
            pos1++;
            pos2--;
        }

        return mutated;
    }
}