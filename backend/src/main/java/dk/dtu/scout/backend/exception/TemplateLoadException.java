package dk.dtu.scout.backend.exception;


public class TemplateLoadException extends RuntimeException {
    public TemplateLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}