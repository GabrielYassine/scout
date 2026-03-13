package dk.dtu.scout.acceptance;

import dk.dtu.scout.Parameter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Component
@Scope("prototype")
public class ElitistAcceptance implements AcceptanceRule {

    @Override
    public String id() {
        return "elitist";
    }

    @Override
    public String displayName() {
        return "Elitist Acceptance";
    }

    @Override
    public String description() {
        return "Accepts only solutions that are better than or equal to the current solution";
    }

    @Override
    public List<Parameter> params() {
        return List.of();
    }

    @Override
    public boolean accept(double currentFitness, double candidateFitness, int iteration, Random rng) {
        return candidateFitness >= currentFitness;
    }
}
