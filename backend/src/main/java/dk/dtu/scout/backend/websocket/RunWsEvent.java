package dk.dtu.scout.backend.websocket;

public record RunWsEvent(String type, String runId, String message) {
    public static RunWsEvent connected(String runId) {
        return new RunWsEvent("RUN_CONNECTED", runId, "Run session connected");
    }

    public static RunWsEvent disconnected(String runId) {
        return new RunWsEvent("RUN_DISCONNECTED", runId, "Run session disconnected");
    }

    public static RunWsEvent finished(String runId) {
        return new RunWsEvent("RUN_FINISHED", runId, "Run finished");
    }
}
