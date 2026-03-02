package dk.dtu.scout.mutation;

import dk.dtu.scout.Parameter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Component
public class TwoOptMutation implements Mutation<int[]> {

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

    @Override
    public int[] mutate(int[] solution, Random rng) {
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