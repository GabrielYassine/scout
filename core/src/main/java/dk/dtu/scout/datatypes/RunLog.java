package dk.dtu.scout.datatypes;

import java.util.ArrayList;
import java.util.List;

public class RunLog<S> {
    private final List<IterationSnapshot> snapshots = new ArrayList<>();
    private S bestSolution;
    private double bestFitness;

    public void log(int iteration, double bestFitness) {
        snapshots.add(new IterationSnapshot(iteration, bestFitness));
        this.bestFitness = bestFitness;
    }

    public List<IterationSnapshot> getSnapshots() { return snapshots; }
    public S getBestSolution() { return bestSolution; }
    public double getBestFitness() { return bestFitness; }
    public void setBestSolution(S bestSolution) { this.bestSolution = bestSolution; }
    public void setBestFitness(double bestFitness) { this.bestFitness = bestFitness;}
}
