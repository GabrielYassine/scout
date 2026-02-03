package dk.dtu.scout.problems;

import java.util.Random;

/**
 * Implementation of the LeadingOnes problem, where the goal is to maximize the consecutive number of 1s from first bit.
 * @author s235257
 */
public class LeadingOnesProblem implements Problem<boolean[]>{

    private final int n;

    public LeadingOnesProblem(int n) {
        this.n = n;
    }

    /**
     * Generates a random binary string of length n.
     * @return A random boolean array representing the solution.
     */
    @Override
    public boolean[] randomSolution(Random rng) {
        boolean[] randomBits = new boolean[n];
        for (int i = 0; i < n; i++) {
            randomBits[i] = rng.nextBoolean();
        }
        return randomBits;
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
