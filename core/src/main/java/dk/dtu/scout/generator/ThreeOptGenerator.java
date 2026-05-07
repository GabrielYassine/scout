package dk.dtu.scout.generator;

import dk.dtu.scout.dto.Parameter;
import dk.dtu.scout.State;
import dk.dtu.scout.datatypes.StateKeys;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

/**
 * Generator that creates a new permutation by applying a random 3-opt style mutation.
 * The generator splits the permutation into four segments and then reconnects
 * the middle segments in one of several possible ways. T
 * @author s230632
 */
@Component
@Scope("prototype")
public class ThreeOptGenerator implements Generator<int[]> {

    private State state;

    @Override
    public void init(State state) {
        this.state = state;
    }

    @Override
    public String id() {
        return "3opt";
    }

    @Override
    public String displayName() {
        return "3-Opt Mutation";
    }

    @Override
    public String description() {
        return "Applies a random 3-opt style reconnection to a permutation.";
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
     * Generates a mutated permutation using a random 3-opt style move.
     *
     * @param rng random number generator used to choose cut points and reconnection type
     * @return mutated permutation
     */
    @Override
    public int[] generate(Random rng) {
        Object baseObj = state.get(StateKeys.OFFSPRING_BASE);
        if (baseObj == null) {
            baseObj = state.get(StateKeys.SELECTED_PARENT_1);
        }

        if (!(baseObj instanceof int[] tour)) {
            throw new IllegalStateException("ThreeOptGenerator requires 'offspringBase' or 'selectedParent1' in state");
        }

        int n = tour.length;
        if (n < 4) {
            return tour.clone();
        }

        int i = rng.nextInt(n - 3);
        int j = i + 1 + rng.nextInt(n - i - 2);
        int k = j + 1 + rng.nextInt(n - j - 1);

        int[] a = slice(tour, 0, i);
        int[] b = slice(tour, i, j);
        int[] c = slice(tour, j, k);
        int[] d = slice(tour, k, n);

        int move = 1 + rng.nextInt(7);

        return switch (move) {
            case 1 -> concat(a, reverse(b), c, d);
            case 2 -> concat(a, b, reverse(c), d);
            case 3 -> concat(a, reverse(b), reverse(c), d);
            case 4 -> concat(a, c, b, d);
            case 5 -> concat(a, reverse(c), b, d);
            case 6 -> concat(a, c, reverse(b), d);
            default -> concat(a, reverse(c), reverse(b), d);
        };
    }

    /**
     * Copies a slice of an array from index from, inclusive, to index to, exclusive.
     *
     * @param arr source array
     * @param from start index, inclusive
     * @param to end index, exclusive
     * @return copied slice
     */
    private int[] slice(int[] arr, int from, int to) {
        int[] out = new int[to - from];
        System.arraycopy(arr, from, out, 0, to - from);
        return out;
    }
    /**
     * Returns a reversed copy of the given array.
     * @param arr array to reverse
     * @return reversed copy
     */
    private int[] reverse(int[] arr) {
        int[] out = arr.clone();
        int l = 0;
        int r = out.length - 1;
        while (l < r) {
            int tmp = out[l];
            out[l] = out[r];
            out[r] = tmp;
            l++;
            r--;
        }
        return out;
    }
    /**
     * Concatenates multiple array segments into one permutation.
     * @param parts array segments to concatenate
     * @return concatenated array
     */
    private int[] concat(int[]... parts) {
        int total = 0;
        for (int[] part : parts) {
            total += part.length;
        }

        int[] out = new int[total];
        int pos = 0;
        for (int[] part : parts) {
            System.arraycopy(part, 0, out, pos, part.length);
            pos += part.length;
        }
        return out;
    }
}