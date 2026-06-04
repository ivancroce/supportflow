package com.supportflow.escalation.client;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Test seam for the Confluence client. See {@link FakeJiraClient}. */
@Component
@ConditionalOnProperty(name = "supportflow.integrations.client", havingValue = "fake")
public class FakeConfluenceClient implements ConfluenceClient {

    private ConfluencePage nextResponse = new ConfluencePage("655361", "https://wiki.example/pages/655361");
    private RuntimeException nextException;
    private int callCount = 0;
    private ConfluencePageRequest lastRequest;

    @Override
    public ConfluencePage createKnownIssue(ConfluencePageRequest request) {
        callCount++;
        lastRequest = request;
        if (nextException != null) {
            throw nextException;
        }
        return nextResponse;
    }

    public void willReturn(ConfluencePage response) {
        this.nextResponse = response;
        this.nextException = null;
    }

    public void willThrow(RuntimeException ex) {
        this.nextException = ex;
    }

    public int callCount() {
        return callCount;
    }

    public ConfluencePageRequest lastRequest() {
        return lastRequest;
    }

    public void reset() {
        nextResponse = new ConfluencePage("655361", "https://wiki.example/pages/655361");
        nextException = null;
        callCount = 0;
        lastRequest = null;
    }
}
