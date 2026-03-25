package dk.dtu.scout.backend.websocket;

import dk.dtu.scout.backend.dto.run.BatchRunResponse;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RunStatusService {

    private final Set<String> finishedRuns = ConcurrentHashMap.newKeySet();
    private final Map<String, BatchRunResponse> finishedResponses = new ConcurrentHashMap<>();

    public void markFinished(String runId, BatchRunResponse response) {
        if (runId != null) {
            finishedRuns.add(runId);
            if (response != null) {
                finishedResponses.put(runId, response);
            }
        }
    }

    public boolean isFinished(String runId) {
        return runId != null && finishedRuns.contains(runId);
    }

    public BatchRunResponse getFinishedResponse(String runId) {
        return runId == null ? null : finishedResponses.get(runId);
    }
}
