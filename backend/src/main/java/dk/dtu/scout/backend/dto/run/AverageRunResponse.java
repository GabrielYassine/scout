package dk.dtu.scout.backend.dto.run;

import java.util.List;
import java.util.Map;

/**
 * DTO for the response of the "average run" endpoint, which provides aggregated data across multiple runs.
 * @param iterations the list of iteration numbers at which data was recorded.
 * @param evaluations the list of evaluation counts corresponding to the iterations.
 * @param series a map where each key is a series name and the value is the values for that series. Usually a list.
 */
public record AverageRunResponse(
    List<Integer> iterations,
    List<Integer> evaluations,
    Map<String, List<Double>> series
) {}
