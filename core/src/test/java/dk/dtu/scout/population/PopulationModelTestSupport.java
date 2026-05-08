package dk.dtu.scout.population;

import dk.dtu.scout.State;
import dk.dtu.scout.crossover.Crossover;
import dk.dtu.scout.datatypes.StateKeys;
import dk.dtu.scout.dto.EvaluatedSolution;
import dk.dtu.scout.dto.Parameter;
import dk.dtu.scout.generator.Generator;
import dk.dtu.scout.parentSelectionRule.ParentSelectionRule;
import dk.dtu.scout.problems.Problem;
import dk.dtu.scout.searchSpace.SearchSpace;
import dk.dtu.scout.selection.SelectionRule;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.function.Supplier;

final class PopulationModelTestSupport {

    private PopulationModelTestSupport() {
    }

    static Queue<String> queue(String... values) {
        return new ArrayDeque<>(List.of(values));
    }

    static PopulationModelContext<String> stringContext(
        SearchSpace<String> space,
        Problem<String> problem,
        Supplier<Generator<String>> generatorFactory,
        ParentSelectionRule<String> parentSelection,
        SelectionRule<String> selection,
        Crossover<String> crossover,
        State sharedState
    ) {
        return new PopulationModelContext<>(
            generatorFactory,
            parentSelection,
            crossover,
            selection,
            space,
            problem,
            new Random(1234L),
            sharedState
        );
    }

    static PopulationModelContext<Integer> integerContext(
        SearchSpace<Integer> space,
        Problem<Integer> problem,
        Supplier<Generator<Integer>> generatorFactory,
        ParentSelectionRule<Integer> parentSelection,
        SelectionRule<Integer> selection,
        Crossover<Integer> crossover
    ) {
        State sharedState = new State();
        sharedState.update(Map.of(
            StateKeys.PROBLEM, problem,
            StateKeys.DIMENSION, space.dimension(),
            StateKeys.SEARCH_SPACE_ID, space.id()
        ));

        return new PopulationModelContext<>(
            generatorFactory,
            parentSelection,
            crossover,
            selection,
            space,
            problem,
            new Random(1234L),
            sharedState
        );
    }

    record TestSearchSpace(Queue<String> solutions) implements SearchSpace<String> {
        @Override
        public String randomSolution(Random rng) {
            return solutions.remove();
        }

        @Override
        public int dimension() {
            return 1;
        }

        @Override
        public String id() {
            return "test-space";
        }

        @Override
        public String displayName() {
            return "Test Space";
        }

        @Override
        public String description() {
            return "Test search space";
        }

        @Override
        public List<Parameter> params() {
            return List.of();
        }
    }

    record TestProblem(Map<String, Double> fitnessValues) implements Problem<String> {
        @Override
        public double fitness(String solution) {
            return fitnessValues.getOrDefault(solution, 0.0);
        }

        @Override
        public boolean isOptimal(double fitness) {
            return false;
        }

        @Override
        public String id() {
            return "test-problem";
        }

        @Override
        public String displayName() {
            return "Test Problem";
        }

        @Override
        public String description() {
            return "Test problem";
        }

        @Override
        public List<Parameter> params() {
            return List.of();
        }
    }

    record TestGenerator(Queue<String> children) implements Generator<String> {
        @Override
        public String generate(Random rng) {
            return children.remove();
        }

        @Override
        public String id() {
            return "test-generator";
        }

        @Override
        public String displayName() {
            return "Test Generator";
        }

        @Override
        public String description() {
            return "Test generator";
        }

        @Override
        public List<Parameter> params() {
            return List.of();
        }
    }

    static final class TestCrossover implements Crossover<String> {
        private final String child;
        private boolean called;

        TestCrossover(String child) {
            this.child = child;
        }

        boolean wasCalled() {
            return called;
        }

        @Override
        public String crossover(Random rng) {
            called = true;
            return child;
        }

        @Override
        public String id() {
            return "test-crossover";
        }

        @Override
        public String displayName() {
            return "Test Crossover";
        }

        @Override
        public String description() {
            return "Test crossover";
        }

        @Override
        public List<Parameter> params() {
            return List.of();
        }
    }

    static class FirstTwoParentSelection<S> implements ParentSelectionRule<S> {
        @Override
        public List<EvaluatedSolution<S>> selectParents(List<EvaluatedSolution<S>> population, Random rng) {
            return List.of(population.get(0), population.get(1));
        }

        @Override
        public String id() {
            return "first-two";
        }

        @Override
        public String displayName() {
            return "First Two";
        }

        @Override
        public String description() {
            return "Selects first two parents";
        }

        @Override
        public List<Parameter> params() {
            return List.of();
        }
    }

    static class KeepBestSelection<S> implements SelectionRule<S> {
        @Override
        public List<EvaluatedSolution<S>> select(
            List<EvaluatedSolution<S>> parents,
            List<EvaluatedSolution<S>> children,
            int mu,
            int iteration,
            Random rng
        ) {
            List<EvaluatedSolution<S>> combined = new ArrayList<>();
            combined.addAll(parents);
            combined.addAll(children);
            combined.sort(Comparator.comparingDouble(EvaluatedSolution<S>::fitness).reversed());
            return new ArrayList<>(combined.subList(0, mu));
        }

        @Override
        public String id() {
            return "keep-best";
        }

        @Override
        public String displayName() {
            return "Keep Best";
        }

        @Override
        public String description() {
            return "Keeps best";
        }

        @Override
        public List<Parameter> params() {
            return List.of();
        }
    }

    static final class KeepExistingParentsSelection<S> extends KeepBestSelection<S> {
        @Override
        public List<EvaluatedSolution<S>> select(
            List<EvaluatedSolution<S>> parents,
            List<EvaluatedSolution<S>> children,
            int mu,
            int iteration,
            Random rng
        ) {
            return parents;
        }
    }

    static final class SelectWeakParentsSelection<S> extends KeepBestSelection<S> {
        @Override
        public List<EvaluatedSolution<S>> select(
            List<EvaluatedSolution<S>> parents,
            List<EvaluatedSolution<S>> children,
            int mu,
            int iteration,
            Random rng
        ) {
            return List.of(parents.get(1), children.getFirst());
        }
    }

    static class SequentialSearchSpace implements SearchSpace<Integer> {
        private int next;

        SequentialSearchSpace(int start) {
            this.next = start;
        }

        @Override
        public Integer randomSolution(Random rng) {
            return next++;
        }

        @Override
        public int dimension() {
            return 10;
        }

        @Override
        public String id() {
            return "integer-space";
        }

        @Override
        public String displayName() {
            return "Integer space";
        }

        @Override
        public String description() {
            return "Integer test space";
        }

        @Override
        public List<Parameter> params() {
            return List.of();
        }
    }

    static class FixedValuesSearchSpace implements SearchSpace<Integer> {
        private final int[] values;
        private int index;

        FixedValuesSearchSpace(int... values) {
            this.values = values;
        }

        @Override
        public Integer randomSolution(Random rng) {
            return values[index++ % values.length];
        }

        @Override
        public int dimension() {
            return 10;
        }

        @Override
        public String id() {
            return "fixed-values";
        }

        @Override
        public String displayName() {
            return "Fixed values";
        }

        @Override
        public String description() {
            return "Fixed test values";
        }

        @Override
        public List<Parameter> params() {
            return List.of();
        }
    }

    static class IdentityProblem implements Problem<Integer> {
        @Override
        public double fitness(Integer solution) {
            return solution;
        }

        @Override
        public boolean isOptimal(double fitness) {
            return false;
        }

        @Override
        public String id() {
            return "identity";
        }

        @Override
        public String displayName() {
            return "Identity";
        }

        @Override
        public String description() {
            return "Fitness equals value";
        }

        @Override
        public List<Parameter> params() {
            return List.of();
        }
    }

    static class IncrementGenerator implements Generator<Integer> {
        private State state;

        @Override
        public void init(State state) {
            this.state = state;
        }

        @Override
        public Integer generate(Random rng) {
            return ((Number) state.get(StateKeys.OFFSPRING_BASE)).intValue() + 10;
        }

        @Override
        public Map<String, Object> getStateVariables(State state) {
            Object current = state.get(StateKeys.CURRENT);

            if (current == null) {
                return Map.of();
            }

            return Map.of("generatorSeenCurrent", current);
        }

        @Override
        public String id() {
            return "increment-generator";
        }

        @Override
        public String displayName() {
            return "Increment generator";
        }

        @Override
        public String description() {
            return "Adds ten";
        }

        @Override
        public List<Parameter> params() {
            return List.of();
        }
    }

    static final class FixedCrossover implements Crossover<Integer> {
        private final int value;
        int calls;

        FixedCrossover(int value) {
            this.value = value;
        }

        @Override
        public Integer crossover(Random rng) {
            calls++;
            return value;
        }

        @Override
        public String id() {
            return "fixed-crossover";
        }

        @Override
        public String displayName() {
            return "Fixed crossover";
        }

        @Override
        public String description() {
            return "Returns fixed value";
        }

        @Override
        public List<Parameter> params() {
            return List.of();
        }
    }

    static class DuplicateParentSelection<S> implements ParentSelectionRule<S> {
        @Override
        public List<EvaluatedSolution<S>> selectParents(List<EvaluatedSolution<S>> parents, Random rng) {
            return List.of(parents.getFirst(), parents.getFirst());
        }

        @Override
        public String id() {
            return "duplicate-parent-selection";
        }

        @Override
        public String displayName() {
            return "Duplicate parent selection";
        }

        @Override
        public String description() {
            return "Duplicates first parent";
        }

        @Override
        public List<Parameter> params() {
            return List.of();
        }
    }

    static class KeepParentsSelectionRule<S> implements SelectionRule<S> {
        @Override
        public List<EvaluatedSolution<S>> select(
            List<EvaluatedSolution<S>> parents,
            List<EvaluatedSolution<S>> children,
            int mu,
            int iteration,
            Random rng
        ) {
            return new ArrayList<>(parents.subList(0, mu));
        }

        @Override
        public String id() {
            return "keep-parents";
        }

        @Override
        public String displayName() {
            return "Keep parents";
        }

        @Override
        public String description() {
            return "Keeps parents";
        }

        @Override
        public List<Parameter> params() {
            return List.of();
        }
    }
}