package dk.dtu.scout.problems;

import dk.dtu.scout.Parameter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Implementation of the LeadingOnes problem, where the goal is to maximize the consecutive number of 1s from first bit.
 * @author s235257
 */

@Component
public class LeadingOnesProblem implements Problem<boolean[]>{

    private int n = 100;

    public LeadingOnesProblem() {}

    @Override
    public void configure(Map<String, Object> params) {
        if (params == null) return;
        if (params.containsKey("n")) {
            this.n = ((Number) params.get("n")).intValue();
        }
    }

    @Override
    public String id() {
        return "leadingones";
    }

    @Override
    public String displayName() {
        return "LeadingOnes";
    }

    @Override
    public String description() {
        return "Maximize the number of leading ones in a bitstring";
    }

    @Override
    public List<Parameter> params() {
        return List.of();
    }


    /**
     * Calculates the fitness of a given solution, defined as the number of consecutive 1s starting from first bit.
     * @param solution the binary string solution to evaluate.
     * @return the fitness value, which is the consecutive count of 1s in the solution.
     */
    @Override
    public double fitness(boolean[] solution) {
        int count = 0;
        for (boolean bit : solution) {
            if (bit) {
                count++;
            } else {
                return count;
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
