package com.supportflow.escalation.client;

public class JiraException extends IntegrationException {

    public JiraException(String message) {
        super(message);
    }

    public JiraException(String message, Throwable cause) {
        super(message, cause);
    }
}
