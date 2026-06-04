package com.supportflow.escalation.client;

/**
 * Creates a Confluence "Known Issue" page (an Investigating stub) for an escalated ticket in a given
 * space. The {@code bodyText} is plain text; the implementation renders it into Confluence storage
 * (XHTML) format.
 */
public interface ConfluenceClient {

    ConfluencePage createKnownIssue(ConfluencePageRequest request);

    record ConfluencePageRequest(String spaceKey, String title, String bodyText) {}

    record ConfluencePage(String id, String url) {}
}
