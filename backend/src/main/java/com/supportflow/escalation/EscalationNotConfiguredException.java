package com.supportflow.escalation;

import java.util.List;

/**
 * Thrown when a ticket is escalated but its Project is missing one or more required targets
 * (Jira project key, Qase project code, Confluence space key). Escalation fails fast with this
 * before any external call is made (see ADR 0003); mapped to HTTP 422.
 */
public class EscalationNotConfiguredException extends RuntimeException {

    private final List<String> missingTargets;

    public EscalationNotConfiguredException(List<String> missingTargets) {
        super("Project is missing escalation targets: " + String.join(", ", missingTargets));
        this.missingTargets = List.copyOf(missingTargets);
    }

    public List<String> getMissingTargets() {
        return missingTargets;
    }
}
