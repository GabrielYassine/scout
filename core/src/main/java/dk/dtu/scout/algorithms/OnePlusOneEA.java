package dk.dtu.scout.algorithms;

import dk.dtu.scout.Parameter;
import dk.dtu.scout.acceptance.AcceptanceRule;
import dk.dtu.scout.mutation.Mutation;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

/**
 * Implementation of the (1+1) Evolutionary Algorithm for optimizing solutions of type S.
 * @param <S> The type of solutions being optimized.
 * @author s235257
 */

@Component
public class OnePlusOneEA<S> implements Algorithm<S> {

    private Mutation<S> mutation;
    private AcceptanceRule acceptance;

    public OnePlusOneEA() {}

    public void setMutation(Mutation<S> mutation) {
        this.mutation = mutation;
    }
    public void setAcceptance(AcceptanceRule acceptance) {
        this.acceptance = acceptance;
    }

    @Override
    public String id() {
        return "1p1-ea";
    }

    @Override
    public String displayName() {
        return "(1+1) EA";
    }

    @Override
    public String description() {
        return "A simple evolutionary algorithm";
    }

    @Override
    public List<Parameter> params() {
        return List.of();
    }

    @Override
    public S propose(S parent, int iteration, Random rng) {
        return mutation.mutate(parent, rng);
    }

    @Override
    public boolean accept(double parentFitness, double childFitness, int iteration, Random rng) {
        return acceptance.accept(parentFitness, childFitness, iteration, rng);
    }
}