package dk.dtu.scout.backend.service;

import dk.dtu.scout.backend.dto.RunRequest;
import dk.dtu.scout.backend.exception.BadRequestException;
import org.springframework.stereotype.Service;

/**
 * Validates run requests and derives logging cadence defaults.
 */
@Service
public class RunRequestValidator {

    public void validate(RunRequest request) {
        if (request == null) {
            throw new BadRequestException("Request must be provided");
        }
        if (request.searchSpaceId() == null || request.searchSpaceId().isBlank()) {
            throw new BadRequestException("Search space must be specified");
        }
        if (request.problemIds() == null || request.problemIds().isEmpty()) {
            throw new BadRequestException("Problem must be specified");
        }
        if (request.generatorId() == null || request.generatorId().isBlank()) {
            throw new BadRequestException("Generator must be specified");
        }
        if (request.populationModelId() == null || request.populationModelId().isBlank()) {
            throw new BadRequestException("Population model must be specified");
        }
        if (request.selectionRuleId() == null || request.selectionRuleId().isBlank()) {
            throw new BadRequestException("Selection rule must be specified");
        }
        if (request.parentSelectionRuleId() == null || request.parentSelectionRuleId().isBlank()) {
            throw new BadRequestException("Parent selection rule must be specified");
        }
        if (request.stopConditionIds() == null || request.stopConditionIds().isEmpty()) {
            throw new BadRequestException("Stop condition must be specified");
        }
        if (request.runId() == null || request.runId().isBlank()) {
            throw new BadRequestException("runId must be specified");
        }
        if (request.runTimes() <= 0) {
            throw new BadRequestException("runTimes must be positive");
        }
    }

    /**
     * Derives the logging interval based on a fraction of max iterations.
     * @param request The run request containing stop condition parameters
     * @return The number of iterations between log updates, defaulting to 10 or 0.1% of max iterations
     */
    public int resolveLogEveryIterations(RunRequest request) {
        int logEvery = 10;
        if (request.stopConditionParams() != null && request.stopConditionIds().contains("max-iterations")) {
            Object maxIterationsObj = request.stopConditionParams().get("maxIterations");
            if (maxIterationsObj != null) {
                int maxIterations = ((Number) maxIterationsObj).intValue();
                logEvery = Math.max(10, maxIterations / 1000); // Log every 0.1% of iterations, with a minimum of every 10 iterations
            }
        }
        return logEvery;
    }
}
