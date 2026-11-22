package org.gmp.serviceabonnements.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.CompletionException;

/**
 * Unwraps {@link CompletionException} thrown when a {@link java.util.concurrent.CompletableFuture}
 * completes exceptionally (e.g. TimeLimiter fallback) so the underlying
 * {@link ResponseStatusException} is mapped to the correct HTTP status instead of 500.
 */
@RestControllerAdvice
public class ChaosExceptionAdvice {

    @ExceptionHandler(CompletionException.class)
    public ResponseEntity<ChaosError> handleCompletionException(CompletionException ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof ResponseStatusException rse) {
            return ResponseEntity.status(rse.getStatusCode())
                    .body(new ChaosError(rse.getStatusCode().value(), rse.getReason()));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ChaosError(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        cause != null ? cause.getMessage() : ex.getMessage()));
    }

    public record ChaosError(int status, String message) {}
}
