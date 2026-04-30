package dk.dtu.scout.selection;

import dk.dtu.scout.dto.EvaluatedSolution;
import dk.dtu.scout.dto.Parameter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

@Component
@Scope("prototype")
public class MuCommaLambdaSelection<S> implements SelectionRule<S> {

    @Override
    public String id() {
        return "mu-comma-lambda";
    }

    @Override
    public String displayName() {
        return "(mu,lambda) Selection";
    }

    @Override
    public String description() {
        return "Selects the best mu solutions from the children only";
    }

    @Override
    public List<Parameter> params() {
        return List.of();
    }

    @Override
    public List<EvaluatedSolution<S>> select(
        List<EvaluatedSolution<S>> parents,
        List<EvaluatedSolution<S>> children,
        int mu,
        int iteration,
        Random rng
    ) {
        if (children == null || children.isEmpty()) {
            throw new IllegalStateException("(mu,lambda) selection requires at least one child");
        }

        if (mu <= 0) {
            throw new IllegalArgumentException("mu must be positive");
        }

        if (children.size() < mu) {
            throw new IllegalArgumentException("(mu,lambda) selection requires lambda >= mu. Got mu=" + mu + " but only " + children.size() + " children were generated.");
        }

        List<EvaluatedSolution<S>> sortedChildren = new ArrayList<>(children);
        sortedChildren.sort(Comparator.comparingDouble(EvaluatedSolution<S>::fitness).reversed());

        return new ArrayList<>(sortedChildren.subList(0, mu));
    }
}