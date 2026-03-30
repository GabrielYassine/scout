package dk.dtu.scout.parentSelectionRule;

import dk.dtu.scout.EvaluatedSolution;
import dk.dtu.scout.Parameter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Component
@Scope("prototype")
public class BestParentSelection<S> implements ParentSelectionRule<S> {

    @Override
    public String id() {
        return "best-parent";
    }

    @Override
    public String displayName() {
        return "Best Parent Selection";
    }

    @Override
    public String description() {
        return "Selects the parent with the highest fitness as the representative for offspring generation.";
    }

    @Override
    public List<Parameter> params() {
        return List.of();
    }

    @Override
    public EvaluatedSolution<S> select(List<EvaluatedSolution<S>> parents, Random rng) {
        if (parents == null || parents.isEmpty()) {
            throw new IllegalStateException("No parents available for parent selection");
        }
        EvaluatedSolution<S> best = parents.getFirst();
        for (int i = 1; i < parents.size(); i++) {
            if (parents.get(i).fitness() > best.fitness()) {
                best = parents.get(i);
            }
        }
        return best;
    }
}
