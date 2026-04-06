package dk.dtu.scout.backend.util;

import dk.dtu.scout.ScoutComponent;
import dk.dtu.scout.Parameter;
import dk.dtu.scout.backend.dto.RunRequest;
import dk.dtu.scout.backend.dto.catalog.CatalogResponse;
import dk.dtu.scout.backend.dto.catalog.ComponentDef;
import dk.dtu.scout.backend.dto.catalog.ParamDef;
import dk.dtu.scout.backend.dto.error.ErrorResponse;
import dk.dtu.scout.backend.dto.run.*;
import dk.dtu.scout.backend.dto.template.ExperimentTemplateDto;
import dk.dtu.scout.backend.dto.permutation.CityDto;
import dk.dtu.scout.backend.dto.permutation.TSPDto;
import dk.dtu.scout.backend.dto.series.SeriesResponse;
import dk.dtu.scout.logging.LoggedSeries;
import dk.dtu.scout.TSPInstance;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ViewMapper {

    private ViewMapper() {
    }

    public static TSPDto toTspDto(TSPInstance instance) {
        return toTspDto(instance, null, 0.0);
    }

    public static TSPDto toTspDto(TSPInstance instance, int[] tour, double tourLength) {
        if (instance == null) {
            throw new IllegalArgumentException("instance must not be null");
        }
        List<CityDto> cities = toCityDtos(instance.getCoordinates());
        return new TSPDto(
            instance.getName(),
            instance.getDimension(),
            cities,
            tour,
            tourLength
        );
    }

    public static List<CityDto> toCityDtos(double[][] coordinates) {
        if (coordinates == null) {
            return List.of();
        }
        List<CityDto> cities = new ArrayList<>(coordinates.length);
        for (int i = 0; i < coordinates.length; i++) {
            cities.add(toCityDto(i, coordinates[i][0], coordinates[i][1]));
        }
        return cities;
    }

    public static CityDto toCityDto(int id, double x, double y) {
        return new CityDto(id, x, y);
    }

    public static ParamDef toParamDef(Parameter param) {
        if (param == null) {
            return null;
        }
        return new ParamDef(
            param.key(),
            param.label(),
            param.type(),
            param.defaultValue(),
            param.min(),
            param.max()
        );
    }

    public static ComponentDef toComponentDef(String kind, ScoutComponent component) {
        if (component == null) {
            throw new IllegalArgumentException("component must not be null");
        }
        return new ComponentDef(
            kind,
            component.id(),
            component.displayName(),
            component.description(),
            component.params().stream().map(ViewMapper::toParamDef).toList(),
            component.supportedSearchSpaces()
        );
    }

    public static CatalogResponse toCatalogResponse(
        List<ComponentDef> searchSpaces,
        List<ComponentDef> problems,
        List<ComponentDef> generators,
        List<ComponentDef> selectionRules,
        List<ComponentDef> populationModels,
        List<ComponentDef> parentSelectionRules,
        List<ComponentDef> crossovers,
        List<ComponentDef> stopConditions,
        List<ComponentDef> observers
    ) {
        return new CatalogResponse(
            searchSpaces,
            problems,
            generators,
            selectionRules,
            populationModels,
            parentSelectionRules,
            crossovers,
            stopConditions,
            observers
        );
    }

    public static ErrorResponse toErrorResponse(int status, String error, String message, String path) {
        return new ErrorResponse(status, error, message, path);
    }

    public static RunResponse toRunResponse(
        String searchSpaceId,
        String problemId,
        List<Integer> iterations,
        List<Integer> evaluations,
        Map<String, LoggedSeries<?>> series,
        double runtimeMs,
        int finalEvaluations
    ) {
        return new RunResponse(
            searchSpaceId,
            problemId,
            iterations,
            evaluations,
            toSeriesResponses(series),
            runtimeMs,
            finalEvaluations
        );
    }

    public static RunGroupResponse toRunGroupResponse(int runIndex, long seed, List<RunResponse> runs) {
        return new RunGroupResponse(runIndex, seed, runs);
    }

    public static BatchSummaryResponse toBatchSummaryResponse(
        Map<String, AverageRunResponse> averageByProblem,
        Map<String, SeriesBoxPlotResponse> bestFitnessBoxPlotsByProblem,
        Map<String, Double> averageRunTimeByProblem
    ) {
        return new BatchSummaryResponse( averageByProblem, bestFitnessBoxPlotsByProblem, averageRunTimeByProblem);
    }

    public static BatchRunResponse toBatchRunResponse(
        String runId,
        List<RunGroupResponse> batches,
        BatchSummaryResponse summary
    ) {
        return new BatchRunResponse(runId, batches, summary);
    }

    public static AverageRunResponse toAverageRunResponse(
        List<Integer> iterations,
        List<Integer> evaluations,
        Map<String, List<Double>> series
    ) {
        return new AverageRunResponse(iterations, evaluations, series);
    }



    public static SeriesResponse<?> toSeriesResponse(LoggedSeries<?> series) {
        if (series == null) {
            return null;
        }
        return new SeriesResponse<>(series.getMode(), series.getValues());
    }

    public static Map<String, SeriesResponse<?>> toSeriesResponses(Map<String, LoggedSeries<?>> series) {
        if (series == null || series.isEmpty()) {
            return Map.of();
        }
        Map<String, SeriesResponse<?>> out = new LinkedHashMap<>();
        for (Map.Entry<String, LoggedSeries<?>> entry : series.entrySet()) {
            SeriesResponse<?> response = toSeriesResponse(entry.getValue());
            if (response == null) continue;
            out.put(entry.getKey(), response);
        }
        return out;
    }

    public static ExperimentTemplateDto toExperimentTemplateDto(
        String id,
        String displayName,
        String description,
        Map<String, Object> runRequest
    ) {
        return new ExperimentTemplateDto(id, displayName, description, runRequest);
    }

    public static RunRequest toRunRequest(
        String searchSpaceId,
        Map<String, Object> searchSpaceParams,
        List<String> problemIds,
        Map<String, Object> problemParams,
        String generatorId,
        Map<String, Object> generatorParams,
        String populationModelId,
        Map<String, Object> populationModelParams,
        String selectionRuleId,
        Map<String, Object> selectionRuleParams,
        String  parentSelectionRuleId,
        Map<String, Object> parentSelectionRuleParams,
        String  crossoverId,
        Map<String, Object> crossoverParams,
        List<String> observerIds,
        Map<String, Object> observerParams,
        List<String> stopConditionIds,
        Map<String, Object> stopConditionParams,
        long seed,
        int runTimes,
        String runId,
        int wsUpdateEveryIterations
    ) {
        return new RunRequest(
            searchSpaceId,
            searchSpaceParams,
            problemIds,
            problemParams,
            generatorId,
            generatorParams,
            populationModelId,
            populationModelParams,
            selectionRuleId,
            selectionRuleParams,
            parentSelectionRuleId,
            parentSelectionRuleParams,
            crossoverId,
            crossoverParams,
            observerIds,
            observerParams,
            stopConditionIds,
            stopConditionParams,
            seed,
            runTimes,
            runId,
            wsUpdateEveryIterations
        );
    }
}
