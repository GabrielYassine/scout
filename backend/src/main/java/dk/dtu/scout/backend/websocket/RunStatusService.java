package dk.dtu.scout.backend.websocket;

import dk.dtu.scout.backend.dto.run.BatchRunResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class RunStatusService {

    private enum StatusType {
        FINISHED,
        FAILED
    }

    private record RunStatus(StatusType type, BatchRunResponse response, String message) {}

    private final int maxEntries;
    private final Map<String, RunStatus> statuses = new LinkedHashMap<>();

    public RunStatusService(@Value("${run.status.maxEntries:500}") int maxEntries) {
        this.maxEntries = Math.max(1, maxEntries);
    }

    public void markFinished(String runId, BatchRunResponse response) {
        if (runId == null) {
            return;
        }
        update(runId, new RunStatus(StatusType.FINISHED, response, null));
    }

    public void markFailed(String runId, String message) {
        if (runId == null) {
            return;
        }
        update(runId, new RunStatus(StatusType.FAILED, null, message));
    }

    public boolean isFinished(String runId) {
        RunStatus status = get(runId);
        return status != null && status.type == StatusType.FINISHED;
    }

    public boolean isFailed(String runId) {
        RunStatus status = get(runId);
        return status != null && status.type == StatusType.FAILED;
    }

    public BatchRunResponse getFinishedResponse(String runId) {
        RunStatus status = get(runId);
        return status == null ? null : status.response;
    }

    public String getFailedMessage(String runId) {
        RunStatus status = get(runId);
        return status == null ? null : status.message;
    }

    private RunStatus get(String runId) {
        if (runId == null) {
            return null;
        }
        synchronized (statuses) {
            return statuses.get(runId);
        }
    }

    private void update(String runId, RunStatus status) {
        synchronized (statuses) {
            statuses.put(runId, status);
            trim();
        }
    }

    private void trim() {
        while (statuses.size() > maxEntries) {
            Iterator<Map.Entry<String, RunStatus>> iterator = statuses.entrySet().iterator();
            if (iterator.hasNext()) {
                iterator.next();
                iterator.remove();
            } else {
                break;
            }
        }
    }
}
