package dk.dtu.scout.backend.integrationtests.support;

import dk.dtu.scout.State;
import dk.dtu.scout.datatypes.StateKeys;
import dk.dtu.scout.dto.EvaluatedSolution;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class CoreTestFixtures {

    private CoreTestFixtures() {
    }

    public static State stateWithBase(Object base) {
        State state = new State();
        state.update(Map.of(StateKeys.OFFSPRING_BASE, base));
        return state;
    }

    public static State stateWithBaseAndDimension(Object base, int dimension) {
        State state = new State();
        state.update(Map.of(
            StateKeys.OFFSPRING_BASE, base,
            StateKeys.DIMENSION, dimension
        ));
        return state;
    }

    public static State stateWithDimension(int dimension) {
        State state = new State();
        state.update(Map.of(StateKeys.DIMENSION, dimension));
        return state;
    }

    public static State stateWithParents(Object parent1, Object parent2) {
        State state = new State();
        state.update(Map.of(
            StateKeys.SELECTED_PARENT_1, parent1,
            StateKeys.SELECTED_PARENT_2, parent2
        ));
        return state;
    }

    public static <S> EvaluatedSolution<S> evaluated(S value, double fitness) {
        return new EvaluatedSolution<>(value, fitness);
    }

    public static int[] sorted(int[] values) {
        int[] copy = values.clone();
        Arrays.sort(copy);
        return copy;
    }

    public static int countDifferences(boolean[] first, boolean[] second) {
        int count = 0;

        for (int i = 0; i < first.length; i++) {
            if (first[i] != second[i]) {
                count++;
            }
        }

        return count;
    }

    public static final class FixedIntRandom extends Random {
        private final int[] values;
        private int index;

        public FixedIntRandom(int... values) {
            this.values = values;
        }

        @Override
        public int nextInt(int bound) {
            return Math.floorMod(values[index++ % values.length], bound);
        }
    }

    public static final class FixedDoubleRandom extends Random {
        private final double[] values;
        private int index;

        public FixedDoubleRandom(double... values) {
            this.values = values;
        }

        @Override
        public double nextDouble() {
            return values[index++ % values.length];
        }
    }

    public static final class FixedBooleanRandom extends Random {
        private final boolean[] values;
        private int index;

        public FixedBooleanRandom(boolean... values) {
            this.values = values;
        }

        @Override
        public boolean nextBoolean() {
            return values[index++ % values.length];
        }
    }

    public static <T> List<T> list(T... values) {
        return List.of(values);
    }
}