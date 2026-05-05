package dk.dtu.scout.backend.util;

import dk.dtu.scout.ScoutComponent;
import dk.dtu.scout.backend.dto.request.RunRequest;
import dk.dtu.scout.backend.dto.catalog.CatalogResponse;
import dk.dtu.scout.backend.dto.catalog.ComponentDef;
import dk.dtu.scout.backend.dto.catalog.ParamDef;
import dk.dtu.scout.backend.dto.error.ErrorResponse;
import dk.dtu.scout.backend.dto.run.AverageRunResponse;
import dk.dtu.scout.backend.dto.run.RunGroupResponse;
import dk.dtu.scout.backend.dto.run.RunResponse;
import dk.dtu.scout.backend.dto.run.SeriesResponse;
import dk.dtu.scout.dto.Parameter;
import dk.dtu.scout.logging.LoggedSeries;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for converting internal backend objects into DTOs used by the frontend.
 * This keeps DTO construction in one place, so controllers and services do not need
 * to know the exact response object structure.
 * The class only contains static mapping methods and should not be instantiated.
 * @author s235257 & Ahmed
 */
public final class ViewMapper {

    private ViewMapper() {
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
            param.max(),
            param.options()
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
            List<Integer> evaluations,
            Map<String, LoggedSeries<?>> series,
            double runtimeMs,
            int totalEvaluations
    ) {
        return new RunResponse(
            searchSpaceId,
            problemId,
            evaluations,
            toSeriesResponses(series),
            runtimeMs,
            totalEvaluations
        );
    }

    public static RunGroupResponse toRunGroupResponse(int runIndex, long seed, List<RunResponse> runs) {
        return new RunGroupResponse(runIndex, seed, runs);
    }

    public static AverageRunResponse toAverageRunResponse(List<Integer> evaluations, Map<String, List<Double>> series) {
        return new AverageRunResponse(evaluations, series);
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
            if (response != null) {
                out.put(entry.getKey(), response);
            }
        }

        return out;
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
        String parentSelectionRuleId,
        Map<String, Object> parentSelectionRuleParams,
        String crossoverId,
        Map<String, Object> crossoverParams,
        List<String> observerIds,
        Map<String, Object> observerParams,
        List<String> stopConditionIds,
        Map<String, Object> stopConditionParams,
        long seed,
        int runTimes,
        String sessionId,
        String runId,
        int logEveryIterations,
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
            sessionId,
            runId,
            logEveryIterations,
            wsUpdateEveryIterations
        );
    }
}