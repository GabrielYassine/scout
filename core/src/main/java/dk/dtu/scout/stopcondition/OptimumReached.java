package dk.dtu.scout.stopcondition;

import dk.dtu.scout.dto.Parameter;
import dk.dtu.scout.State;
import dk.dtu.scout.datatypes.StateKeys;
import dk.dtu.scout.problems.Problem;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Stop condition that checks if the optimal solution has been found.
 * @author s235257 & s230632
 */
@Component
@Scope("prototype")
public class OptimumReached<S> implements StopCondition<S> {

    private Problem<?> problem;

    public OptimumReached() {}

    public void setProblem(Problem<?> problem) {
        this.problem = problem;
    }

    @Override
    public String id() {
        return "optimum-reached";
    }

    @Override
    public String displayName() {
        return "Optimum Reached";
    }

    @Override
    public String description() {
        return "Stops when the optimal solution is found";
    }

    @Override
    public List<Parameter> params() {
        return List.of();
    }

    @Override
    public List<String> supportedSearchSpaces() {
        return List.of("bitstring", "permutation", "route-list");
    }

    @Override
    public void init(State state) {
        Object problemObj = state.get(StateKeys.PROBLEM);
        this.problem = (Problem<?>) problemObj;
    }
    /**
     * Checks if the stop condition is met based on whether the best fitness value found so far is optimal according to the problem definition.
     * @param iteration current iteration number
     * @param evaluations total number of fitness evaluations performed (not used in this condition)
     * @param bestFitness best fitness value found so far
     * @param bestSolution best solution found so far (not used in this condition)
     * @return true if the best fitness is optimal, false otherwise
     */
    @Override
    public boolean shouldStop(int iteration, int evaluations, double bestFitness, S bestSolution) {
        return problem.isOptimal(bestFitness);
    }
}