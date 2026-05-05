package dk.dtu.scout.generator;

import dk.dtu.scout.State;
import dk.dtu.scout.datatypes.StateKeys;
import dk.dtu.scout.dto.Parameter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Component
@Scope("prototype")
public class SwapGenerator implements Generator<int[]> {

    private State state;

    @Override
    public void init(State state) {
        this.state = state;
    }

    @Override
    public String id() {
        return "swap";
    }

    @Override
    public String displayName() {
        return "Swap Mutation";
    }

    @Override
    public String description() {
        return "Randomly swaps two elements in the permutation.";
    }

    @Override
    public List<Parameter> params() {
        return List.of();
    }

    @Override
    public List<String> supportedSearchSpaces() {
        return List.of("permutation");
    }

    @Override
    public int[] generate(Random rng) {
        int[] mutated = ((int[]) state.get(StateKeys.OFFSPRING_BASE)).clone();

        if (mutated.length < 2) {
            return mutated;
        }

        int pos1 = rng.nextInt(mutated.length);
        int pos2 = rng.nextInt(mutated.length);

        while (pos2 == pos1) {
            pos2 = rng.nextInt(mutated.length);
        }

        int temp = mutated[pos1];
        mutated[pos1] = mutated[pos2];
        mutated[pos2] = temp;

        return mutated;
    }
}