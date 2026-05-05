package dk.dtu.scout.observer;

import dk.dtu.scout.dto.Parameter;
import dk.dtu.scout.logging.IterationSnapshot;
import dk.dtu.scout.logging.RunLog;
import dk.dtu.scout.logging.SeriesMode;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Scope("prototype")
public class HypercubeObserver implements Observer<boolean[]> {

    @Override
    public String id() {
        return "hypercube";
    }

    @Override
    public String displayName() {
        return "Hypercube (2D Projection)";
    }

    @Override
    public String description() {
        return "Maps the current solution to (x,y) in the Boolean hypercube projection.";
    }

    @Override
    public List<Parameter> params() {
        return List.of();
    }

    @Override
    public List<String> supportedSearchSpaces() {
        return List.of("bitstring");
    }

    @Override
    public void onStep(IterationSnapshot<boolean[]> state, RunLog log) {
        Point2D point = map(state.currentSolution());

        log.putSeries("hypercubeX", point.x(), SeriesMode.ALL);
        log.putSeries("hypercubeY", point.y(), SeriesMode.ALL);
    }

    static Point2D map(boolean[] bits) {
        int n = bits.length;

        if (n == 0) {
            return new Point2D(0.0, 0.0);
        }

        int ones = 0;
        long indexSum = 0;

        for (int i = 0; i < n; i++) {
            if (bits[i]) {
                ones++;
                indexSum += i;
            }
        }

        double y = (double) ones / n;
        double t = 0.5;

        if (ones != 0 && ones != n) {
            double minSum = ones * (ones - 1) / 2.0;
            double maxSum = ones * (2.0 * n - ones - 1) / 2.0;

            t = (indexSum - minSum) / (maxSum - minSum);
            t = Math.max(0.0, Math.min(1.0, t));
        }

        double xRaw = 2.0 * t - 1.0;
        double scale = 7.0;
        double u = (2.0 * y - 1.0) * scale;
        double envelope = Math.exp(-(u * u) / 8.0);

        return new Point2D(xRaw * envelope, y);
    }

    record Point2D(double x, double y) {
    }
}