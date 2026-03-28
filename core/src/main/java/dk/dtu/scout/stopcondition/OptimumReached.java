package dk.dtu.scout.stopcondition;

import dk.dtu.scout.Parameter;
import dk.dtu.scout.State;
import dk.dtu.scout.problems.Problem;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

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
        return List.of("bitstring");
    }

    @Override
    public void init(State state) {
        if (state == null) {
            return;
        }
        Object problemObj = state.get("problem");
        if (problemObj instanceof Problem<?> p) {
            this.problem = p;
        }
    }

    @Override
    public boolean shouldStop(int iteration, int evaluations, double bestFitness, S bestSolution) {
        return problem.isOptimal(bestFitness);
    }
}
