package dk.dtu.scout.parentSelectionRule;

import dk.dtu.scout.dto.EvaluatedSolution;
import dk.dtu.scout.dto.Parameter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Parent selection rule that selects parents uniformly at random.
 * The rule selects two parents from the current evaluated parent population.
 * Selection is done with replacement, meaning the same parent can be selected
 * twice. This allows the rule to work even when the population contains only
 * one parent.
 * @param <S> solution representation type
 * @author s230632
 */
@Component
@Scope("prototype")
public class RandomParentSelection<S> implements ParentSelectionRule<S> {

    @Override
    public String id() {
        return "random-parents";
    }

    @Override
    public String displayName() {
        return "Random Parent Selection";
    }

    @Override
    public String description() {
        return "Chooses parents uniformly at random from the current population.";
    }

    @Override
    public List<Parameter> params() {
        return List.of();
    }
    /**
     * Selects two parents uniformly at random from the parent population.
     * @param parents evaluated parent population to select from
     * @param rng random number generator used for parent sampling
     * @return two randomly selected parents
     */
    @Override
    public List<EvaluatedSolution<S>> selectParents(List<EvaluatedSolution<S>> parents, Random rng) {
        if (parents == null || parents.isEmpty()) {
            throw new IllegalStateException("No parents available");
        }

        List<EvaluatedSolution<S>> chosen = new ArrayList<>(2);

        for (int i = 0; i < 2; i++) {
            chosen.add(parents.get(rng.nextInt(parents.size())));
        }

        return chosen;
    }
}