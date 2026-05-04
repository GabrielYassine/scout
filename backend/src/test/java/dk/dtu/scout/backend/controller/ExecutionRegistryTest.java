package dk.dtu.scout.backend.controller;

import dk.dtu.scout.backend.service.ExecutionRegistry;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Future;

import static org.mockito.Mockito.*;

class ExecutionRegistryTest {

    private final ExecutionRegistry executionRegistry = new ExecutionRegistry();

    @Test
    void registerActive_cancelsPreviousTaskForSameSession() {
        Future<?> previousFuture = mock(Future.class);
        Future<?> newFuture = mock(Future.class);

        executionRegistry.registerActive("session-1", "run-1", previousFuture);
        executionRegistry.registerActive("session-1", "run-2", newFuture);

        verify(previousFuture).cancel(true);
        verify(newFuture, never()).cancel(true);
    }

    @Test
    void registerActive_doesNotCancelWhenNoPreviousTaskExists() {
        Future<?> future = mock(Future.class);

        executionRegistry.registerActive("session-1", "run-1", future);

        verify(future, never()).cancel(true);
    }

    @Test
    void registerActive_handlesPreviousTaskWithNullFuture() {
        executionRegistry.registerActive("session-1", "run-1", null);

        Future<?> newFuture = mock(Future.class);
        executionRegistry.registerActive("session-1", "run-2", newFuture);

        verify(newFuture, never()).cancel(true);
    }
}