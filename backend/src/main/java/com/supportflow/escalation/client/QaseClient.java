package com.supportflow.escalation.client;

/** Creates a Qase test case for an escalated ticket in a given project (by project code). */
public interface QaseClient {

    QaseCase createCase(QaseCaseRequest request);

    record QaseCaseRequest(String projectCode, String title, String description) {}

    record QaseCase(String id, String url) {}
}
