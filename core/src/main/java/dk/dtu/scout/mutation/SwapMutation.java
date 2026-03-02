package dk.dtu.scout.mutation;

import dk.dtu.scout.Parameter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Component
public class SwapMutation implements Mutation<int[]> {

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
        return "Randomly swap two elements in the permutation";
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
    public int[] mutate(int[] solution, Random rng) {
        int[] mutated = solution.clone();

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