package dk.dtu.scout.backend.integrationtests.support;

public record PreparedExecution(
    String sessionId,
    String executionId
) {
}