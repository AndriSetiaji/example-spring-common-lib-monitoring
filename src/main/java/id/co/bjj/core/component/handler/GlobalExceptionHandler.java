package id.co.bjj.core.component.handler;

import io.opentelemetry.api.trace.Span;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        Span currentSpan = Span.current();
        currentSpan.recordException(e);
        currentSpan.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR);
        return ResponseEntity.internalServerError().body("An error occurred: " + e.getMessage());
    }
}
