package am.asukiasyan.booking.controller.handler;

import am.asukiasyan.booking.exception.BadRequestException;
import am.asukiasyan.booking.exception.ConflictException;
import am.asukiasyan.booking.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@Slf4j
@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex) {
        log.warn("Not found: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex) {
        log.warn("Conflict: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleServerError(Exception ex) {
        log.error("Unexpected error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");
    }

    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleValidation(BindException ex) {
        log.warn("Validation failed: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "Validation failed");
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message) {
        var response = new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                message,
                Instant.now());
        return ResponseEntity.status(status).body(response);
    }
}
