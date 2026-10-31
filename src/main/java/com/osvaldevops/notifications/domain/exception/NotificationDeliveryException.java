package com.osvaldevops.notifications.domain.exception;

/**
 * Domain exception thrown when dispatching a notification to the outbound channel fails.
 */
public class NotificationDeliveryException extends RuntimeException {

    public NotificationDeliveryException(String message) {
        super(message);
    }

    public NotificationDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}

