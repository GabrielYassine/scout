package dk.dtu.scout.datatypes;

import java.util.ArrayList;
import java.util.List;

/**
 * Logs the progress of an optimization run by storing snapshots of the best fitness found at each iteration.
 */

public class RunLog {
    private final List<IterationSnapshot> iterationSnapshots = new ArrayList<>();

    public RunLog() {
    }

    /**
     * Logs the best fitness at a given iteration.
     * @param iteration is the current iteration number
     * @param bestFitness is the best fitness found so far
     */
    public void log(int iteration, double bestFitness) {
        iterationSnapshots.add(new IterationSnapshot(iteration, bestFitness));
    }

    public List<IterationSnapshot> getIterationSnapshots() {
        return List.copyOf(iterationSnapshots);
    }

}
