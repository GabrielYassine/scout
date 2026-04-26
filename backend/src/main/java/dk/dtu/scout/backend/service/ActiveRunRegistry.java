package dk.dtu.scout.backend.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * Tracks active asynchronous run and study tasks.
 * A session can only have one active task at a time, so registering a new task cancels the previous task for that same session.
 * Also guards against duplicate websocket start messages for the same run or study id which can happen when a client reconnects.
 * @author s235257 & Ahmed
 */
@Component
public class ActiveRunRegistry {

    private record ActiveTask(String id, Future<?> future) {
    }

    private final ConcurrentHashMap<String, ActiveTask> activeBySession = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> startedRunIds = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> startedStudyIds = new ConcurrentHashMap<>();

    public boolean markRunStarted(String runId) {
        return startedRunIds.putIfAbsent(runId, Boolean.TRUE) == null;
    }

    public boolean markStudyStarted(String studyId) {
        return startedStudyIds.putIfAbsent(studyId, Boolean.TRUE) == null;
    }

    public void register(String sessionId, String taskId, Future<?> future) {
        ActiveTask previous = activeBySession.get(sessionId);
        if (previous != null && previous.future() != null) {
            previous.future().cancel(true);
        }

        activeBySession.put(sessionId, new ActiveTask(taskId, future));
    }

    public void finishRun(String sessionId, String runId) {
        startedRunIds.remove(runId);
        removeActiveTask(sessionId, runId);
    }

    public void finishStudy(String sessionId, String studyId) {
        startedStudyIds.remove(studyId);
        removeActiveTask(sessionId, studyId);
    }

    private void removeActiveTask(String sessionId, String taskId) {
        activeBySession.computeIfPresent(sessionId, (sid, active) -> taskId.equals(active.id()) ? null : active);
    }
}