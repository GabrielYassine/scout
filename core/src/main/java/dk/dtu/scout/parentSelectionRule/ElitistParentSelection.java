package dk.dtu.scout.parentSelectionRule;

import dk.dtu.scout.dto.EvaluatedSolution;
import dk.dtu.scout.dto.Parameter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Parent selection rule that selects the best individuals from the current parent population.
 * The rule returns the two highest-fitness parents. If the population only contains
 * one parent, the same parent is returned twice so crossover-based components can
 * still receive two parent references.
 * @param <S> solution representation type
 * @author s230632
 */
@Component
@Scope("prototype")
public class ElitistParentSelection<S> implements ParentSelectionRule<S> {

    @Override
    public String id() {
        return "elitist-parents";
    }

    @Override
    public String displayName() {
        return "Elitist Parent Selection";
    }

    @Override
    public String description() {
        return "Chooses the best parents from the current population.";
    }

    @Override
    public List<Parameter> params() {
        return List.of();
    }

    /**
     * Selects the two best parents based on fitness.
     * @param parents evaluated parent population to select from
     * @param rng random number generator, unused because this rule is deterministic
     * @return the two highest-fitness parents, or the same parent twice if only one exists
     */
    @Override
    public List<EvaluatedSolution<S>> selectParents(List<EvaluatedSolution<S>> parents, Random rng) {
        if (parents == null || parents.isEmpty()) {
            throw new IllegalStateException("No parents available");
        }

        List<EvaluatedSolution<S>> sorted = new ArrayList<>(parents);
        sorted.sort(Comparator.comparingDouble(EvaluatedSolution<S>::fitness).reversed());

        if (sorted.size() == 1) {
            return List.of(sorted.getFirst(), sorted.getFirst());
        }

        return List.of(sorted.get(0), sorted.get(1));
    }
}