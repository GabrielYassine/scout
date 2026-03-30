package dk.dtu.scout.parentSelectionRule;

import dk.dtu.scout.EvaluatedSolution;
import dk.dtu.scout.Parameter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Component
@Scope("prototype")
public class RandomParentSelection<S> implements ParentSelectionRule<S> {

    @Override
    public String id() {
        return "random-parent";
    }

    @Override
    public String displayName() {
        return "Random Parent Selection";
    }

    @Override
    public String description() {
        return "Selects a uniformly random parent as the representative for offspring generation.";
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
        return parents.get(rng.nextInt(parents.size()));
    }
}
