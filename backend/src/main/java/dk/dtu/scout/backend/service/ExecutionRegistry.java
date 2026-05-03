package dk.dtu.scout.backend.service;

import dk.dtu.scout.backend.dto.request.RunRequest;
import dk.dtu.scout.backend.dto.request.RuntimeStudyRequest;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * Tracks the lifecycle of prepared and active executions.
 * A run or runtime study is first prepared through REST, where the finalized
 * request is validated and stored. It is then consumed when the frontend starts the execution through websocket.
 * A session can only have one active task at a time, so registering a new active task cancels the previous active task for the same session.
 * @author s235257 & Ahmed
 */
@Component
public class ExecutionRegistry {

    private enum ExecutionType {
        RUN,
        STUDY
    }

    public record PreparedExecutionIds(String sessionId, String executionId) {
    }

    private record ActiveTask(String id, Future<?> future) {
    }

    private record WebSocketBinding(String sessionId, String taskId, ExecutionType type) {
    }

    private final ConcurrentHashMap<String, RunRequest> preparedRuns = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, RuntimeStudyRequest> preparedStudies = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, ActiveTask> activeBySession = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, WebSocketBinding> websocketBindings = new ConcurrentHashMap<>();

    public PreparedExecutionIds prepareIds(String requestedSessionId) {
        String sessionId = requestedSessionId != null && !requestedSessionId.isBlank() ? requestedSessionId : UUID.randomUUID().toString();
        String executionId = UUID.randomUUID().toString();
        removePreparedForSession(sessionId);
        return new PreparedExecutionIds(sessionId, executionId);
    }

    /**
     * Stores a finalized and validated run request until the websocket start message arrives.
     * @param request finalized run request
     */
    public void storePreparedRun(RunRequest request) {
        if (request == null || request.runId() == null || request.runId().isBlank()) {
            throw new IllegalArgumentException("Prepared run must have a runId");
        }

        preparedRuns.put(request.runId(), request);
    }

    /**
     * Stores a finalized and validated runtime study request until the websocket start message arrives.
     * @param request finalized runtime study request
     */
    public void storePreparedStudy(RuntimeStudyRequest request) {
        if (request == null || request.studyId() == null || request.studyId().isBlank()) {
            throw new IllegalArgumentException("Prepared runtime study must have a studyId");
        }

        preparedStudies.put(request.studyId(), request);
    }

    /**
     * Consumes a prepared run request.
     * The request is removed when consumed so the same prepared run cannot be started twice.
     * @param runId prepared run id
     * @param sessionId browser session id that owns the prepared run
     * @return the finalized run request
     */
    public RunRequest consumePreparedRun(String runId, String sessionId) {
        RunRequest request = preparedRuns.remove(runId);

        if (request == null) {
            throw new IllegalArgumentException("Prepared run was not found or has already been started");
        }

        if (!request.sessionId().equals(sessionId)) {
            throw new IllegalArgumentException("Prepared run does not belong to this session");
        }

        return request;
    }

    /**
     * Consumes a prepared runtime study request.
     * The request is removed when consumed so the same prepared study cannot be started twice.
     * @param studyId prepared study id
     * @param sessionId browser session id that owns the prepared study
     * @return the finalized runtime study request
     */
    public RuntimeStudyRequest consumePreparedStudy(String studyId, String sessionId) {
        RuntimeStudyRequest request = preparedStudies.remove(studyId);

        if (request == null) {
            throw new IllegalArgumentException("Prepared runtime study was not found or has already been started");
        }

        if (!request.sessionId().equals(sessionId)) {
            throw new IllegalArgumentException("Prepared runtime study does not belong to this session");
        }

        return request;
    }

    /**
     * Removes prepared executions for a session.
     * This prevents old prepared-but-never-started executions from accumulating
     * when the user prepares a new execution from the same browser session.
     * @param sessionId browser session id
     */
    public void removePreparedForSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }

        preparedRuns.entrySet().removeIf(entry -> sessionId.equals(entry.getValue().sessionId()));
        preparedStudies.entrySet().removeIf(entry -> sessionId.equals(entry.getValue().sessionId()));
    }

    /**
     * Registers a new active task for the given session and task id.
     * If the same session already has an active task, the previous task is cancelled.
     * @param sessionId browser session id
     * @param taskId run or study id
     * @param future asynchronous task future
     */
    public void registerActive(String sessionId, String taskId, Future<?> future) {
        ActiveTask previous = activeBySession.get(sessionId);
        if (previous != null && previous.future() != null) {
            previous.future().cancel(true);
        }

        activeBySession.put(sessionId, new ActiveTask(taskId, future));
    }

    /**
     * Attaches a websocket connection to a prepared or active run.
     * @param websocketSessionId Spring websocket session id
     * @param sessionId browser session id
     * @param runId run id
     */
    public void attachRunWebSocket(String websocketSessionId, String sessionId, String runId) {
        attachWebSocket(websocketSessionId, sessionId, runId, ExecutionType.RUN);
    }

    /**
     * Attaches a websocket connection to a prepared or active runtime study.
     * @param websocketSessionId Spring websocket session id
     * @param sessionId browser session id
     * @param studyId runtime study id
     */
    public void attachStudyWebSocket(String websocketSessionId, String sessionId, String studyId) {
        attachWebSocket(websocketSessionId, sessionId, studyId, ExecutionType.STUDY);
    }

    private void attachWebSocket(String websocketSessionId, String sessionId, String taskId, ExecutionType type) {
        if (websocketSessionId == null || websocketSessionId.isBlank()) {
            return;
        }

        if (sessionId == null || sessionId.isBlank()) {
            return;
        }

        if (taskId == null || taskId.isBlank()) {
            return;
        }

        websocketBindings.put(websocketSessionId, new WebSocketBinding(sessionId, taskId, type));
    }

    /**
     * Handles a websocket disconnect.
     * If the disconnected websocket owns a prepared execution, that prepared execution is forgotten.
     * If it owns an active execution, the active task is cancelled and removed.
     * @param websocketSessionId Spring websocket session id
     */
    public void handleWebSocketDisconnect(String websocketSessionId) {
        WebSocketBinding binding = websocketBindings.remove(websocketSessionId);
        if (binding == null) {
            return;
        }

        if (binding.type() == ExecutionType.RUN) {
            preparedRuns.remove(binding.taskId());
        } else {
            preparedStudies.remove(binding.taskId());
        }

        cancelActiveTask(binding.sessionId(), binding.taskId());
    }

    /**
     * Removes the active run for the given session if it matches the current active task.
     * @param sessionId browser session id
     * @param runId run id
     */
    public void finishRun(String sessionId, String runId) {
        removeActiveTask(sessionId, runId);
    }

    /**
     * Removes the active runtime study for the given session if it matches the current active task.
     * @param sessionId browser session id
     * @param studyId runtime study id
     */
    public void finishStudy(String sessionId, String studyId) {
        removeActiveTask(sessionId, studyId);
    }

    private void cancelActiveTask(String sessionId, String taskId) {
        activeBySession.computeIfPresent(sessionId, (sid, active) -> {
            if (!taskId.equals(active.id())) {
                return active;
            }

            if (active.future() != null) {
                active.future().cancel(true);
            }

            return null;
        });
    }

    /**
     * Removes the active task for the given session if the task id matches.
     * This prevents an old finishing task from removing a newer task from the same session.
     * @param sessionId browser session id
     * @param taskId run or study id
     */
    private void removeActiveTask(String sessionId, String taskId) {
        activeBySession.computeIfPresent(sessionId, (sid, active) -> taskId.equals(active.id()) ? null : active);
    }
}