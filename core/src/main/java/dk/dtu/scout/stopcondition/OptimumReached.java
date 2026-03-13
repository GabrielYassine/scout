package dk.dtu.scout.stopcondition;

import dk.dtu.scout.ConfigurationContext;
import dk.dtu.scout.Parameter;
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


    public void configure(Map<String, Object> params, ConfigurationContext context) {
        if (context.hasProblem()) {
            this.problem = context.getProblem();
        }
        if (params != null && !params.isEmpty()) {
            configure(params);
        }
    }

    @Override
    public boolean shouldStop(int iteration, int evaluations, double bestFitness, S bestSolution) {
        return problem.isOptimal(bestFitness);
    }
}
