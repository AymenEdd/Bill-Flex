package org.gmp.service_notifications.exception;

public class NotificationConflictException extends RuntimeException {

    public NotificationConflictException() {
        super("Une notification de ce type existe déjà pour cette facture.");
    }
}
