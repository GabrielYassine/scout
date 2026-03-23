package dk.dtu.scout.backend.websocket;

import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RunStatusService {

    private final Set<String> finishedRuns = ConcurrentHashMap.newKeySet();

    public void markFinished(String runId) {
        if (runId != null) {
            finishedRuns.add(runId);
        }
    }

    public boolean isFinished(String runId) {
        return runId != null && finishedRuns.contains(runId);
    }
}
