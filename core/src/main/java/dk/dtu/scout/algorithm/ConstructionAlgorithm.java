package dk.dtu.scout.algorithm;

import dk.dtu.scout.construction.ConstructionPolicy;
import dk.dtu.scout.heuristic.HeuristicFunction;
import dk.dtu.scout.pheromone.PheromoneModel;
import dk.dtu.scout.logging.RunLog;
import dk.dtu.scout.logging.RunState;
import dk.dtu.scout.observer.Observer;
import dk.dtu.scout.problems.Problem;
import dk.dtu.scout.searchSpace.SearchSpace;
import dk.dtu.scout.stopcondition.StopCondition;

import java.util.Random;

public class ConstructionAlgorithm<S> implements Algorithm<S> {

    private final PheromoneModel<S> pheromoneModel;
    private final HeuristicFunction<S> heuristicFunction;
    private final ConstructionPolicy<S> constructionPolicy;
    private final int numAnts;
    private final double evaporationRate;
    private final double q; // Pheromone deposit factor

    public ConstructionAlgorithm(
            PheromoneModel<S> pheromoneModel,
            HeuristicFunction<S> heuristicFunction,
            ConstructionPolicy<S> constructionPolicy,
            int numAnts,
            double evaporationRate,
            double q
    ) {
        this.pheromoneModel = pheromoneModel;
        this.heuristicFunction = heuristicFunction;
        this.constructionPolicy = constructionPolicy;
        this.numAnts = numAnts;
        this.evaporationRate = evaporationRate;
        this.q = q;
    }

    @Override
    public RunLog run(
            SearchSpace<S> space,
            Problem<S> problem,
            Random rng,
            StopCondition<S> stop,
            Observer<S> observer
    ) {
        RunLog log = new RunLog();

        // Initialize pheromones and heuristics
        int dimension = space.dimension();
        pheromoneModel.initialize(dimension);
        heuristicFunction.initialize(problem);

        // Initialize best solution
        S best = null;
        double bestFitness = Double.NEGATIVE_INFINITY;

        int iteration = 0;
        int evaluations = 0;

        // Initial state
        RunState<S> initial = new RunState<>(iteration, evaluations, null, Double.NEGATIVE_INFINITY, best, bestFitness, false);
        observer.onStart(initial, log);
        log.tick(iteration, evaluations);
        observer.onStep(initial, log);

        // Main ACO loop
        while (!stop.shouldStop(iteration, evaluations, bestFitness, best)) {
            S iterationBest = null;
            double iterationBestFitness = Double.NEGATIVE_INFINITY;

            // Each ant constructs a solution
            for (int ant = 0; ant < numAnts; ant++) {
                S solution = constructionPolicy.constructSolution(pheromoneModel, heuristicFunction, rng);
                double fitness = problem.fitness(solution);
                evaluations++;

                // Track best solution in this iteration
                if (fitness > iterationBestFitness) {
                    iterationBestFitness = fitness;
                    iterationBest = solution;
                }

                // Track global best
                if (fitness > bestFitness) {
                    bestFitness = fitness;
                    best = solution;
                }
            }

            // Evaporate pheromones
            pheromoneModel.evaporate(evaporationRate);

            // Deposit pheromones on iteration-best solution
            if (iterationBest != null) {
                double depositAmount = q / (-iterationBestFitness); // Negative because fitness is negative tour length
                if (depositAmount > 0) {
                    pheromoneModel.deposit(iterationBest, depositAmount);
                }
            }

            iteration++;

            // Log state
            RunState<S> state = new RunState<>(
                iteration,
                evaluations,
                iterationBest,
                iterationBestFitness,
                best,
                bestFitness,
                iterationBestFitness > bestFitness
            );
            log.tick(iteration, evaluations);
            observer.onStep(state, log);
        }

        // Final state
        RunState<S> finalState = new RunState<>(iteration - 1, evaluations, best, bestFitness, best, bestFitness, false);
        observer.onEnd(finalState, log);

        return log;
    }

    public PheromoneModel<S> getPheromoneModel() {
        return pheromoneModel;
    }

    public HeuristicFunction<S> getHeuristicFunction() {
        return heuristicFunction;
    }

    public ConstructionPolicy<S> getConstructionPolicy() {
        return constructionPolicy;
    }

    public int getNumAnts() {
        return numAnts;
    }

    public double getEvaporationRate() {
        return evaporationRate;
    }

    public double getQ() {
        return q;
    }
}
