package com.supportflow.escalation.client;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Test seam for the Qase client. See {@link FakeJiraClient}. */
@Component
@ConditionalOnProperty(name = "supportflow.integrations.client", havingValue = "fake")
public class FakeQaseClient implements QaseClient {

    private QaseCase nextResponse = new QaseCase("1", "https://app.qase.example/case/SQT-1");
    private RuntimeException nextException;
    private int callCount = 0;
    private QaseCaseRequest lastRequest;

    @Override
    public QaseCase createCase(QaseCaseRequest request) {
        callCount++;
        lastRequest = request;
        if (nextException != null) {
            throw nextException;
        }
        return nextResponse;
    }

    public void willReturn(QaseCase response) {
        this.nextResponse = response;
        this.nextException = null;
    }

    public void willThrow(RuntimeException ex) {
        this.nextException = ex;
    }

    public int callCount() {
        return callCount;
    }

    public QaseCaseRequest lastRequest() {
        return lastRequest;
    }

    public void reset() {
        nextResponse = new QaseCase("1", "https://app.qase.example/case/SQT-1");
        nextException = null;
        callCount = 0;
        lastRequest = null;
    }
}
