package dk.dtu.scout.backend.dto.run;

import java.util.List;

/**
 * DTO for a single data point in a runtime study
 * @param problemSize the size of the problem instance for this data point
 * @param meanEvaluationsToOptimum the mean number of evaluations to reach the optimum across all repetitions for this problem size
 * @param boxPlot a list of 5 values representing the box plot for the number of evaluations to optimum
 * across all repetitions for this problem size, in the order: [min, Q1, median, Q3, max]
 * @author s230632
 */
public record RuntimeStudyPointResponse(
    int problemSize,
    double meanEvaluationsToOptimum,
    List<Double> boxPlot
) {}