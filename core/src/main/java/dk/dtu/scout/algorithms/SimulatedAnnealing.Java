package dk.dtu.scout.algorithms;

import dk.dtu.scout.Parameter;
import dk.dtu.scout.acceptance.AcceptanceRule;
import dk.dtu.scout.logging.RunLog;
import dk.dtu.scout.logging.RunState;
import dk.dtu.scout.mutation.Mutation;
import dk.dtu.scout.observer.Observer;
import dk.dtu.scout.problems.Problem;
import dk.dtu.scout.searchSpace.SearchSpace;
import dk.dtu.scout.stopcondition.StopCondition;
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

    public SimulatedAnnealing() {}

    public void setMutation(Mutation<S> mutation) {
        this.mutation = mutation;
    }

    public void setAcceptance(AcceptanceRule acceptance) {
        this.acceptance = acceptance;
    }

    @Override
    public String id() {
        return "sa";
    }

    @Override
    public String displayName() {
        return "Simulated Annealing";
    }

    @Override
    public String description() {
        return "An optimization algorithm inspired by the annealing process in metallurgy";
    }

    @Override
    public List<Parameter> params() {
        return List.of();
    }

    @Override
    public RunLog run(SearchSpace<S> space, Problem<S> problem, Random rng, StopCondition<S> stop, Observer<S> observer) {
        RunLog log = new RunLog();

        S current = space.randomSolution(rng);
        double currentFitness = problem.fitness(current);

        S best = current;
        double bestFitness = currentFitness;

        int evaluations = 1;

        // Initial state
        RunState<S> initialState = new RunState<>(
                0, evaluations, current, currentFitness, best, bestFitness, false
        );
        observer.onStart(initialState, log);
        log.tick(initialState.iteration());
        observer.onStep(initialState, log);

        int iteration = 1;
        while(!stop.shouldStop(iteration, evaluations, bestFitness, best)){

            S candidate = mutation.mutate(current, rng);
            double candidateFitness = problem.fitness(candidate);
            evaluations++;

            if (candidateFitness > bestFitness) {
                best = candidate;
                bestFitness = candidateFitness;
            }

            boolean accepted = acceptance.accept(currentFitness, candidateFitness, iteration, rng);

            if (accepted) {
                current = candidate;
                currentFitness = candidateFitness;
            }

            RunState<S> state = new RunState<>(
                    iteration, evaluations, current, currentFitness, best, bestFitness, accepted
            );
            log.tick(state.iteration());
            observer.onStep(state, log);

            iteration++;
        }

        RunState<S> finalState = new RunState<>(
                iteration - 1, evaluations, current, currentFitness, best, bestFitness, false
        );
        observer.onEnd(finalState, log);

        return log;
    }
}
