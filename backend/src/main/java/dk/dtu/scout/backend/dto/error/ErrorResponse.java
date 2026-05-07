package dk.dtu.scout.backend.dto.error;

/**
 * DTO for error responses sent to the frontend when an exception occurs.
 * @author s230632
 */
public record ErrorResponse(
    int status,
    String error,
    String message,
    String path
) {}