package dk.dtu.scout.stopcondition;

import dk.dtu.scout.dto.Parameter;
import dk.dtu.scout.State;
import dk.dtu.scout.datatypes.StateKeys;
import dk.dtu.scout.problems.Problem;
import dk.dtu.scout.problems.TSP;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Stop condition that checks if the best fitness has reached or exceeded a specified target value.
 * For TSP/VRP, the target can be interpreted as a distance.
 * @author s235257
 */
@Component
@Scope("prototype")
public class TargetReached<S> implements StopCondition<S> {
    private double targetFitness = 0.0;
    private static final double EPSILON = 1e-9;
    private boolean targetIsDistance = false;
    private Problem<?> problem;

    public TargetReached() {}

    @Override
    public String id() {
        return "target-reached";
    }

    @Override
    public String displayName() {
        return "Target Fitness Reached";
    }

    @Override
    public String description() {
        return "Stops when the best fitness meets or exceeds a target value";
    }

    @Override
    public List<Parameter> params() {
        return List.of(
            new Parameter("targetFitness", "Target fitness", "double", targetFitness, null, null, null),
            new Parameter("targetIsDistance", "Target is distance (TSP)", "boolean", targetIsDistance, null, null, null)
        );
    }

    @Override
    public void configure(Map<String, Object> params) {
        if (params.containsKey("targetFitness")) {
            this.targetFitness = ((Number) params.get("targetFitness")).doubleValue();
        }

        if (params.containsKey("targetIsDistance")) {
            this.targetIsDistance = (boolean) params.get("targetIsDistance");
        }
    }

    @Override
    public void init(State state) {
        Object problemObj = state.get(StateKeys.PROBLEM);
        this.problem = (Problem<?>) problemObj;
    }

    @Override
    public boolean shouldStop(int iteration, int evaluations, double bestFitness, S bestSolution) {
        if (problem instanceof TSP) {
            if (targetIsDistance || targetFitness > 0.0) {
                return bestFitness >= -targetFitness - EPSILON;
            }
        }
        return bestFitness >= targetFitness;
    }
}
