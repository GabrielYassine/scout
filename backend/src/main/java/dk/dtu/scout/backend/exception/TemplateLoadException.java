package dk.dtu.scout.backend.exception;

/**
 * Exception used when backend templates cannot be loaded.
 * This is treated as a server-side error and mapped to HTTP 500 by GlobalExceptionHandler.
 * @author Ahmed
 */
public class TemplateLoadException extends RuntimeException {

    public TemplateLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}