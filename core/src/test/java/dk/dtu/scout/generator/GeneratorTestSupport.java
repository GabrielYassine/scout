package dk.dtu.scout.generator;

import dk.dtu.scout.State;
import dk.dtu.scout.datatypes.StateKeys;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

final class GeneratorTestSupport {

    private GeneratorTestSupport() {
    }

    static State stateWithBase(boolean[] base) {
        State state = new State();
        state.update(Map.of(
            StateKeys.OFFSPRING_BASE, base,
            StateKeys.DIMENSION, base.length
        ));
        return state;
    }

    static State stateWithBase(int[] base) {
        State state = new State();
        state.update(Map.of(StateKeys.OFFSPRING_BASE, base));
        return state;
    }

    static State stateWithRouteBase(Object base) {
        State state = new State();
        state.update(Map.of(StateKeys.OFFSPRING_BASE, base));
        return state;
    }

    static State stateWithSelectedParent(int[] parent) {
        State state = new State();
        state.update(Map.of(StateKeys.SELECTED_PARENT_1, parent));
        return state;
    }

    static State stateWithSelectedRouteParent(Object parent) {
        State state = new State();
        state.update(Map.of(StateKeys.SELECTED_PARENT_1, parent));
        return state;
    }

    static int[] sorted(int[] values) {
        int[] copy = values.clone();
        Arrays.sort(copy);
        return copy;
    }

    static List<Integer> sortedFlattened(List<List<Integer>> routes) {
        List<Integer> values = new ArrayList<>();

        for (List<Integer> route : routes) {
            values.addAll(route);
        }

        Collections.sort(values);
        return values;
    }

    static final class FixedIntRandom extends Random {
        private final int[] values;
        private int index;

        FixedIntRandom(int... values) {
            this.values = values;
        }

        @Override
        public int nextInt(int bound) {
            return Math.floorMod(values[index++], bound);
        }
    }
}