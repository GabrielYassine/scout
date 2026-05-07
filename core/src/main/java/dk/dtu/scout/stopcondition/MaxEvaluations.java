package dk.dtu.scout.stopcondition;

import dk.dtu.scout.dto.Parameter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 *
 * @author s235257 & s230632
 */
@Component
@Scope("prototype")
public class MaxEvaluations<S> implements StopCondition<S> {
    private int maxEvaluations = 10_000;

    public MaxEvaluations() {}

    @Override
    public String id() {
        return "max-evaluations";
    }

    @Override
    public String displayName() {
        return "Max Evaluations";
    }

    @Override
    public String description() {
        return "Stops the algorithm after a maximum number of fitness evaluations";
    }

    @Override
    public List<Parameter> params() {
        return List.of(new Parameter("maxEvaluations", "Max evaluations", "int", maxEvaluations, 1.0, null, null));
    }

    @Override
    public void configure(Map<String, Object> params) {
        if (params.containsKey("maxEvaluations")) {
            this.maxEvaluations = ((Number) params.get("maxEvaluations")).intValue();
        }
    }

    @Override
    public boolean shouldStop(int iteration, int evaluations, double bestFitness, S bestSolution) {
        return evaluations >= maxEvaluations;
    }
}