package dk.dtu.scout.backend.util;

import java.util.List;

public final class StatisticsMath {

    private StatisticsMath() {
    }

    public static double percentile(List<Double> sorted, double p) {
        if (sorted == null || sorted.isEmpty()) {
            return 0.0;
        }
        if (sorted.size() == 1) {
            return sorted.getFirst();
        }

        double index = (p / 100.0) * (sorted.size() - 1);
        int lower = (int) Math.floor(index);
        int upper = (int) Math.ceil(index);

        if (lower == upper) {
            return sorted.get(lower);
        }

        double fraction = index - lower;
        return sorted.get(lower) + fraction * (sorted.get(upper) - sorted.get(lower));
    }

    public static List<Double> fiveNumberSummary(List<Double> sorted) {
        if (sorted == null || sorted.isEmpty()) {
            return List.of();
        }

        return List.of(
                sorted.getFirst(),
                percentile(sorted, 25),
                percentile(sorted, 50),
                percentile(sorted, 75),
                sorted.getLast()
        );
    }
}