package dk.dtu.scout.algorithms;

import dk.dtu.scout.Parameter;
import dk.dtu.scout.acceptance.AcceptanceRule;
import dk.dtu.scout.mutation.Mutation;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

/**
 * Simulated Annealing algorithm using a mutation (neighborhood) and an acceptance rule.
 * @param <S> The type of solutions being optimized.
 *   * @author s230632
 */

@Component
public class SimulatedAnnealing<S> implements Algorithm<S> {

    private Mutation<S> mutation;
    private AcceptanceRule acceptance;

    public void setMutation(Mutation<S> mutation) { this.mutation = mutation; }
    public void setAcceptance(AcceptanceRule acceptance) { this.acceptance = acceptance; }

    @Override
    public String id() { return "sa"; }

    @Override
    public String displayName() { return "Simulated Annealing"; }

    @Override
    public String description() { return "An optimization algorithm inspired by annealing"; }

    @Override
    public List<Parameter> params() { return List.of(); }

    @Override
    public S propose(S parent, int iteration, Random rng) {
        return mutation.mutate(parent, rng);
    }

    @Override
    public boolean accept(double parentFitness, double childFitness, int iteration, Random rng) {
        return acceptance.accept(parentFitness, childFitness, iteration, rng);
    }
}