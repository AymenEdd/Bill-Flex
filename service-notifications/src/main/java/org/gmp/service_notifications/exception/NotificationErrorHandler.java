package org.gmp.service_notifications.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class NotificationErrorHandler {

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            HandlerMethodValidationException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    ResponseEntity<ApiError> invalidRequest(Exception ex) {
        return error(HttpStatus.BAD_REQUEST, "REQUETE_INVALIDE",
                "Vérifier les identifiants positifs, le type et le corps de la requête.");
    }

    @ExceptionHandler(NotificationNotFoundException.class)
    ResponseEntity<ApiError> notFound(NotificationNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, "NOTIFICATION_INTROUVABLE", ex.getMessage());
    }

    @ExceptionHandler(NotificationConflictException.class)
    ResponseEntity<ApiError> conflict(NotificationConflictException ex) {
        return error(HttpStatus.CONFLICT, "NOTIFICATION_EXISTANTE", ex.getMessage());
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ApiError(status.value(), code, message));
    }

    record ApiError(int status, String code, String message) {
    }
}
