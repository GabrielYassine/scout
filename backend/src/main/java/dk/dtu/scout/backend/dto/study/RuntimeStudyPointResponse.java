package dk.dtu.scout.backend.dto.study;

import java.util.List;

public record RuntimeStudyPointResponse(
        int problemSize,
        double meanEvaluationsToOptimum,
        List<Double> boxPlot
) {}