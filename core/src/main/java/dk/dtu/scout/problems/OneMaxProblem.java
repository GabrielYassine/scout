package dk.dtu.scout.problems;

import dk.dtu.scout.Parameter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Implementation of the OneMax problem, where the goal is to maximize the number of 1s in a binary string.
 * @author s235257
 */

@Component
public class OneMaxProblem implements Problem<boolean[]> {

    private int n = 100;

    public OneMaxProblem() {}

    @Override
    public void configure(Map<String, Object> params) {
        if (params == null) return;
        if (params.containsKey("n")) {
            this.n = ((Number) params.get("n")).intValue();
        }
    }

    @Override
    public String id() {
        return "onemax";
    }

    @Override
    public String displayName() {
        return "OneMax";
    }

    @Override
    public String description() {
        return "Maximize the number of ones in a bitstring";
    }

    @Override
    public List<Parameter> params() {
        return List.of();
    }



    /**
     * Calculates the fitness of a given solution, defined as the number of 1s in the binary string.
     * @param solution The binary string solution to evaluate.
     * @return The fitness value, which is the count of 1s in the solution.
     */

    @Override
    public double fitness(boolean[] solution) {
        int count = 0;
        for (boolean bit : solution) {
            if (bit) {
                count++;
            }
        }
        return count;
    }

    /**
     * Determines if the given fitness value represents an optimal solution.
     * @param fitness The fitness value to check.
     * @return True if the fitness equals n (all bits are 1), false otherwise.
     */
    @Override
    public boolean isOptimal(double fitness) {
        return fitness == n;
    }

}
