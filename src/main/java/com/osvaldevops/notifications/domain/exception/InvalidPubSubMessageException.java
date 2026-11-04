package com.osvaldevops.notifications.domain.exception;

/**
 * Domain exception thrown when an incoming Google Cloud Pub/Sub Push message
 * is missing required fields, cannot be decoded from Base64/JSON, or specifies
 * an unsupported event type.
 */
public class InvalidPubSubMessageException extends RuntimeException {

    public InvalidPubSubMessageException(String message) {
        super(message);
    }

    public InvalidPubSubMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}

