package com.supportflow.escalation.client;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Test seam for the Jira client. Tests set the next response (or a failure) and assert how the
 * escalation pipeline reacts. Tracks call count so idempotency tests can prove a retry skipped it.
 */
@Component
@ConditionalOnProperty(name = "supportflow.integrations.client", havingValue = "fake")
public class FakeJiraClient implements JiraClient {

    private JiraIssue nextResponse = new JiraIssue("SFJ-1", "https://jira.example/browse/SFJ-1");
    private RuntimeException nextException;
    private int callCount = 0;
    private JiraIssueRequest lastRequest;

    @Override
    public JiraIssue createIssue(JiraIssueRequest request) {
        callCount++;
        lastRequest = request;
        if (nextException != null) {
            throw nextException;
        }
        return nextResponse;
    }

    public void willReturn(JiraIssue response) {
        this.nextResponse = response;
        this.nextException = null;
    }

    public void willThrow(RuntimeException ex) {
        this.nextException = ex;
    }

    public int callCount() {
        return callCount;
    }

    public JiraIssueRequest lastRequest() {
        return lastRequest;
    }

    public void reset() {
        nextResponse = new JiraIssue("SFJ-1", "https://jira.example/browse/SFJ-1");
        nextException = null;
        callCount = 0;
        lastRequest = null;
    }
}
