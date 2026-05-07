package dk.dtu.scout.backend.service;

import dk.dtu.scout.backend.dto.request.RunRequest;
import dk.dtu.scout.backend.dto.request.RuntimeStudyRequest;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * Tracks the lifecycle of prepared and active executions.
 * A run or runtime study is first prepared through REST, where the finalized
 * request is validated and stored. It is then consumed when the frontend starts the execution through websocket.
 * A session can only have one active task at a time, so registering a new active task cancels the previous active task for the same session.
 * @author s235257
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

        preparedRuns.entrySet().removeIf(entry -> sessionId.equals(entry.getValue().sessionId()));
        preparedStudies.entrySet().removeIf(entry -> sessionId.equals(entry.getValue().sessionId()));

        return new PreparedExecutionIds(sessionId, executionId);
    }

    /**
     * Stores a finalized and validated run request until the websocket start message arrives.
     * @param request finalized run request
     */
    public void storePreparedRun(RunRequest request) {
        preparedRuns.put(request.runId(), request);
    }

    /**
     * Stores a finalized and validated runtime study request until the websocket start message arrives.
     * @param request finalized runtime study request
     */
    public void storePreparedStudy(RuntimeStudyRequest request) {
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
     * Registers a new active task for the given session and task id.
     * If the same session already has an active task, the previous task is cancelled.
     * @param sessionId browser session id
     * @param taskId run or study id
     * @param future asynchronous task future
     */
    public void registerActive(String sessionId, String taskId, Future<?> future) {
        Objects.requireNonNull(future, "future must not be null");

        activeBySession.compute(sessionId, (sid, previous) -> {
            if (previous != null) {
                previous.future().cancel(true);
            }

            return new ActiveTask(taskId, future);
        });
    }

    /**
     * Attaches a websocket connection to a prepared or active run.
     * @param websocketSessionId Spring websocket session id
     * @param sessionId browser session id
     * @param runId run id
     */
    public void attachRunWebSocket(String websocketSessionId, String sessionId, String runId) {
        websocketBindings.put(websocketSessionId, new WebSocketBinding(sessionId, runId, ExecutionType.RUN));
    }

    /**
     * Attaches a websocket connection to a prepared or active runtime study.
     * @param websocketSessionId Spring websocket session id
     * @param sessionId browser session id
     * @param studyId runtime study id
     */
    public void attachStudyWebSocket(String websocketSessionId, String sessionId, String studyId) {
        websocketBindings.put(websocketSessionId, new WebSocketBinding(sessionId, studyId, ExecutionType.STUDY));
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

        activeBySession.computeIfPresent(binding.sessionId(), (sid, active) -> {
            if (!binding.taskId().equals(active.id())) {
                return active;
            }

            active.future().cancel(true);
            return null;
        });
    }

    /**
     * Removes the active run for the given session if it matches the current active task.
     * @param sessionId browser session id
     * @param runId run id
     */
    public void finishRun(String sessionId, String runId) {
        activeBySession.computeIfPresent(sessionId, (sid, active) -> runId.equals(active.id()) ? null : active);
    }

    /**
     * Removes the active runtime study for the given session if it matches the current active task.
     * @param sessionId browser session id
     * @param studyId runtime study id
     */
    public void finishStudy(String sessionId, String studyId) {
        activeBySession.computeIfPresent(sessionId, (sid, active) -> studyId.equals(active.id()) ? null : active);
    }
}