package dk.dtu.scout.generator;

import dk.dtu.scout.Parameter;
import dk.dtu.scout.State;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

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

    @Override
    public int[] generate(Random rng) {
        Object baseObj = state.get("offspringBase");
        if (baseObj == null) {
            baseObj = state.get("selectedParent1");
        }

        if (!(baseObj instanceof int[] tour)) {
            throw new IllegalStateException(
                    "ThreeOptGenerator requires 'offspringBase' or 'selectedParent1' in state"
            );
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
            case 7 -> concat(a, reverse(c), reverse(b), d);
            default -> throw new IllegalStateException("Unexpected 3-opt move: " + move);
        };
    }

    private int[] slice(int[] arr, int from, int to) {
        int[] out = new int[to - from];
        System.arraycopy(arr, from, out, 0, to - from);
        return out;
    }

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