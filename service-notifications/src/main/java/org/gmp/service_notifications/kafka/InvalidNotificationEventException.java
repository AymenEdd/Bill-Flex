package org.gmp.service_notifications.kafka;

class InvalidNotificationEventException extends RuntimeException {

    InvalidNotificationEventException(String message) {
        super(message);
    }
}
