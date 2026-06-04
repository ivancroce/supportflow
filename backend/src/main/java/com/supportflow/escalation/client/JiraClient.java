package com.supportflow.escalation.client;

/**
 * Creates a Jira issue for an escalated ticket in a given project. The {@code description} is plain
 * text; the implementation renders it into Jira Cloud's required ADF document shape.
 */
public interface JiraClient {

    JiraIssue createIssue(JiraIssueRequest request);

    record JiraIssueRequest(String projectKey, String issueType, String summary, String description) {}

    record JiraIssue(String key, String url) {}
}
