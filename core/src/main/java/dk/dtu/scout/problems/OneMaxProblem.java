package dk.dtu.scout.problems;

/**
 * Implementation of the OneMax problem, where the goal is to maximize the number of 1s in a binary string.
 * @author s235257
 */
public class OneMaxProblem implements Problem<boolean[]> {

    private final int n;

    public OneMaxProblem(int n) {
        this.n = n;
    }

    /**
     * Generates a random binary string of length n.
     * @return A random boolean array representing the solution.
     */
    @Override
    public boolean[] randomSolution() {
        boolean[] randomBits = new boolean[n];
        for (int i = 0; i < n; i++) {
            randomBits[i] = Math.random() < 0.5;
        }
        return randomBits;
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
