package com.supportflow.escalation.client;

public class ConfluenceException extends IntegrationException {

    public ConfluenceException(String message) {
        super(message);
    }

    public ConfluenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
