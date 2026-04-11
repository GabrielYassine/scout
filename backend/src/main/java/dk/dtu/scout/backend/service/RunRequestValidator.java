package dk.dtu.scout.backend.service;

import dk.dtu.scout.backend.dto.RunRequest;
import dk.dtu.scout.backend.dto.RuntimeStudyRequest;
import dk.dtu.scout.backend.exception.BadRequestException;
import org.springframework.stereotype.Service;

/**
 * Validates run requests and derives logging cadence defaults.
 */
@Service
public class RunRequestValidator {

    public void runRequestValidator(RunRequest request) {
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
        if (request.logEveryIterations() < 0) {
            throw new BadRequestException("logEveryIterations must be zero or positive");
        }
    }
    public void runtimeStudyRequestValidator(RuntimeStudyRequest request) {
        if (request == null) {
            throw new BadRequestException("Request must be provided");
        }
        if (request.studyId() == null || request.studyId().isBlank()) {
            throw new BadRequestException("studyId must be specified");
        }
        if (request.searchSpaceId() == null || request.searchSpaceId().isBlank()) {
            throw new BadRequestException("Search space must be specified");
        }
        if (request.problemId() == null || request.problemId().isBlank()) {
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
        if (request.stopConditionIds() == null || request.stopConditionIds().isEmpty()|| !request.stopConditionIds().contains("optimum-reached")) {
            throw new BadRequestException("Stop condition must contain 'optimum-reached' and cannot be empty");
        }
        if (request.problemSizes() == null || request.problemSizes().isEmpty()) {
            throw new BadRequestException("At least one problem size must be specified");
        }
        if (request.problemSizes().stream().anyMatch(n -> n == null || n <= 0)) {
            throw new BadRequestException("All problem sizes must be positive");
        }
        if (request.repetitionsPerSize() <= 0) {
            throw new BadRequestException("repetitionsPerSize must be positive");
        }
        if (request.seed() <= 0) {
            throw new BadRequestException("seed must be positive");
        }

        validateRuntimeStudySemantics(request);
    }

    private void validateRuntimeStudySemantics(RuntimeStudyRequest request) {

        if ("tsp".equals(request.problemId()) || "vrp".equals(request.problemId())) {
            throw new BadRequestException(
                    "Runtime study currently supports theoretical size-based problems only"
            );
        }
    }

    /**
     * Derives the logging interval based on a fraction of max iterations.
     * @param request The run request containing stop condition parameters
     * @return The number of iterations between log updates, defaulting to 10 or 0.1% of max iterations
     */
public int resolveLogEveryIterations(RunRequest request) {
    return request.logEveryIterations() > 0 ? request.logEveryIterations() : 10;
}
}
