package dk.dtu.scout.backend.exception;

import dk.dtu.scout.backend.dto.error.ErrorResponse;
import dk.dtu.scout.backend.util.ViewMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

/**
 * Central exception handler for REST controllers.
 * Converts backend exceptions into consistent JSON error responses.
 * @author s230632
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles invalid client requests.
     * @param e the thrown bad request exception
     * @param request the HTTP request that caused the exception
     * @return HTTP 400 response with a structured error body
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException e, HttpServletRequest request) {
        ErrorResponse error = ViewMapper.toErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            e.getMessage(),
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableMessage(HttpMessageNotReadableException e, HttpServletRequest request) {
        ErrorResponse error = ViewMapper.toErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            "Request body is missing or malformed.",
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handles unexpected errors not covered by more specific handlers.
     * @param e the thrown exception
     * @param request the HTTP request that caused the exception
     * @return HTTP 500 response with a structured error body
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedError(Exception e, HttpServletRequest request) {
        ErrorResponse error = ViewMapper.toErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            "An unexpected server error occurred.",
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}