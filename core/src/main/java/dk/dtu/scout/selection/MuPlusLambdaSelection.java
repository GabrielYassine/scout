package dk.dtu.scout.selection;

import dk.dtu.scout.dto.EvaluatedSolution;
import dk.dtu.scout.dto.Parameter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Selection rule for (mu+lambda) evolutionary strategies, which selects the best mu solutions from the combined set of parents and children.
 * @author s235257 & s230632
 */
@Component
@Scope("prototype")
public class MuPlusLambdaSelection<S> implements SelectionRule<S> {

    @Override
    public String id() {
        return "mu-plus-lambda";
    }

    @Override
    public String displayName() {
        return "(mu+lambda) Selection";
    }

    @Override
    public String description() {
        return "Selects the best mu solutions from parents and children combined";
    }

    @Override
    public List<Parameter> params() {
        return List.of();
    }


    /**
     * Selects the next parent population from parents and children combined.
     * The method sorts all candidate solutions by fitness in descending order and
     * returns the best mu solutions.
     * @param parents current evaluated parent population
     * @param children evaluated offspring population
     * @param mu number of solutions to keep for the next generation
     * @param iteration current iteration number, unused by this deterministic rule
     * @param rng random number generator, unused by this deterministic rule
     * @return selected parent population for the next generation
     */
    @Override
    public List<EvaluatedSolution<S>> select(
        List<EvaluatedSolution<S>> parents,
        List<EvaluatedSolution<S>> children,
        int mu,
        int iteration,
        Random rng
    ) {
        List<EvaluatedSolution<S>> combined = new ArrayList<>(parents.size() + children.size());
        combined.addAll(parents);
        combined.addAll(children);
        combined.sort(Comparator.comparingDouble(EvaluatedSolution<S>::fitness).reversed());

        int limit = Math.min(mu, combined.size());
        return new ArrayList<>(combined.subList(0, limit));
    }
}