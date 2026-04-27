package dk.dtu.scout.backend.dto.run;

import java.util.List;

/**
 * DTO for a series of box plots returned to the frontend, where each box plot corresponds to a different evaluation count.
 * @param evaluations a list of evaluation counts corresponding to each box plot
 * @param boxplots a list of box plots, where each box plot is a list of 5 values representing [min, Q1, median, Q3, max]
 * for the number of evaluations to optimum across all repetitions for that evaluation count
 * @author Ahmed
 */
public record SeriesBoxPlotResponse(
    List<Integer> evaluations,
    List<List<Double>> boxplots
) {}