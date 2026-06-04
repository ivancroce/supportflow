package com.supportflow.escalation.client;

/**
 * Base type for a failed call to an external escalation target (Jira / Qase / Confluence). The
 * {@code EscalationService} catches this and records the tool as FAILED in the result envelope,
 * leaving the other tools and a later retry unaffected (see ADR 0003).
 */
public class IntegrationException extends RuntimeException {

    public IntegrationException(String message) {
        super(message);
    }

    public IntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
