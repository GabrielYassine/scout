package dk.dtu.scout.stopcondition;

import dk.dtu.scout.dto.Parameter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Scope("prototype")
public class MaxIterations<S> implements StopCondition<S> {
    private int maxIterations = 10_000;

    public MaxIterations() {}

    @Override
    public String id() {
        return "max-iterations";
    }

    @Override
    public String displayName() {
        return "Max Iterations";
    }

    @Override
    public String description() {
        return "Stops the algorithm after a maximum number of iterations";
    }

    @Override
    public List<Parameter> params() {
        return List.of(new Parameter("maxIterations", "Max iterations", "int", maxIterations, 1.0, null, null));
    }

    @Override
    public void configure(Map<String, Object> params) {
        if (params == null) return;
        if (params.containsKey("maxIterations")) {
            this.maxIterations = ((Number) params.get("maxIterations")).intValue();
        }
    }

    @Override
    public boolean shouldStop(int iteration, int evaluations, double bestFitness, S bestSolution) {
        return iteration >= maxIterations;
    }
}