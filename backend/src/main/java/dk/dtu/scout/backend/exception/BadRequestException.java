package dk.dtu.scout.backend.exception;

/**
 * Exception used when the client sends an invalid request.
 * This is mapped to HTTP 400 by GlobalExceptionHandler.
 * @author Ahmed
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}