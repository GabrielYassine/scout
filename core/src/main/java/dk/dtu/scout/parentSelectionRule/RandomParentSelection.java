package dk.dtu.scout.parentSelectionRule;

import dk.dtu.scout.dto.EvaluatedSolution;
import dk.dtu.scout.dto.Parameter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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