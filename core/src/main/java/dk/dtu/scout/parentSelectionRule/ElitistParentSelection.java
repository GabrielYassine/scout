package dk.dtu.scout.parentSelectionRule;

import dk.dtu.scout.EvaluatedSolution;
import dk.dtu.scout.Parameter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

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

    @Override
    public List<EvaluatedSolution<S>> selectParents(List<EvaluatedSolution<S>> parents, Random rng) {
        if (parents == null || parents.isEmpty()) {
            throw new IllegalStateException("No parents available");
        }

        List<EvaluatedSolution<S>> sorted = new ArrayList<>(parents);
        sorted.sort(Comparator.comparingDouble(EvaluatedSolution<S>::fitness).reversed());

        if (sorted.size() == 1) {
            return List.of(sorted.get(0), sorted.get(0));
        }

        return List.of(sorted.get(0), sorted.get(1));
    }
}