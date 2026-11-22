package org.gmp.service_notifications.exception;

public class NotificationNotFoundException extends RuntimeException {

    public NotificationNotFoundException(Long id) {
        super("Notification introuvable pour l’identifiant " + id + ".");
    }
}
