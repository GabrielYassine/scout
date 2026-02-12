package dk.dtu.scout.stopcondition;

import dk.dtu.scout.Component;

public interface StopCondition<S> extends Component {
    boolean shouldStop(int iteration, int evaluations, double bestFitness, S bestSolution);
}