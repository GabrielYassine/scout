package dk.dtu.scout.stopcondition;

import dk.dtu.scout.dto.Parameter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Stop condition that terminates the algorithm after a specified maximum number of fitness evaluations.
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
    /**
     * Checks if the stop condition is met based on the number of evaluations.
     * @param iteration current iteration number (not used in this condition)
     * @param evaluations total number of fitness evaluations performed
     * @param bestFitness best fitness value found so far (not used in this condition)
     * @param bestSolution best solution found so far (not used in this condition)
     * @return true if the number of evaluations has reached or exceeded the maximum, false otherwise
     */
    @Override
    public boolean shouldStop(int iteration, int evaluations, double bestFitness, S bestSolution) {
        return evaluations >= maxEvaluations;
    }
}