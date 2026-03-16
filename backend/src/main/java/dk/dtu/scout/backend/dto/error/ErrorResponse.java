package dk.dtu.scout.backend.dto.error;

public record ErrorResponse(
        int status,
        String error,
        String message,
        String path
) {}