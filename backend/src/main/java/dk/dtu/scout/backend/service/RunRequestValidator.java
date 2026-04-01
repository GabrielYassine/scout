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
        if (request.searchSpaceId() == null || request.searchSpaceId().isEmpty()) {
            throw new BadRequestException("Search space must be specified");
        }
        if (request.problemId() == null || request.problemId().isEmpty()) {
            throw new BadRequestException("Problem must be specified");
        }
        if (request.runId() == null || request.runId().isBlank()) {
            throw new BadRequestException("runId must be specified");
        }
        if (request.runTimes() <= 0) {
            throw new BadRequestException("runTimes must be positive");
        }
    }

    public int resolveLogEveryIterations(RunRequest request) {
        int maxIterations = 0;
        if (request != null && request.stopConditionParams() != null) {
            Object value = request.stopConditionParams().get("maxIterations");
            if (value instanceof Number n) {
                maxIterations = n.intValue();
            }
        }
        if (maxIterations <= 0) return 1;
        int interval = (int) Math.round(maxIterations * 0.001);
        return Math.max(1, interval);
    }
}
