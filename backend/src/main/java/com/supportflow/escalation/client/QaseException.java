package com.supportflow.escalation.client;

public class QaseException extends IntegrationException {

    public QaseException(String message) {
        super(message);
    }

    public QaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
