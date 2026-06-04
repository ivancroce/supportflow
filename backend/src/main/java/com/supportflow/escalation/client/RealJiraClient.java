package com.supportflow.escalation.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Production Jira Cloud client. Creates a Bug issue via {@code POST /rest/api/3/issue} using Basic
 * auth (Atlassian email + API token). The v3 API requires the description as an Atlassian Document
 * Format (ADF) document, so {@link #toAdf} wraps each line of plain text in a paragraph node.
 *
 * <p>Unlike the Gemini client this is NOT retried: issue creation is not idempotent, so a retry after
 * an ambiguous failure could create a duplicate Jira issue. A failure surfaces as {@link
 * JiraException}, which the escalation service records as a FAILED tool outcome; the user re-runs
 * escalate explicitly and the skip-by-stored-id logic avoids double-creating the tools that succeeded.
 */
@Component
@ConditionalOnProperty(name = "supportflow.integrations.client", havingValue = "real", matchIfMissing = true)
public class RealJiraClient implements JiraClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int SUMMARY_MAX_LENGTH = 255;

    private final RestClient http;
    private final String baseUrl;
    private final String authHeader;

    public RealJiraClient(
            RestClient.Builder builder,
            @Value("${supportflow.integrations.jira.base-url:}") String baseUrl,
            @Value("${supportflow.integrations.atlassian.email:}") String email,
            @Value("${supportflow.integrations.atlassian.api-token:}") String apiToken) {
        this.http = builder.build();
        this.baseUrl = stripTrailingSlash(baseUrl);
        this.authHeader = AtlassianBasicAuth.header(email, apiToken);
    }

    @Override
    public JiraIssue createIssue(JiraIssueRequest request) {
        Map<String, Object> body = Map.of("fields", Map.of(
                "project", Map.of("key", request.projectKey()),
                "issuetype", Map.of("name", request.issueType()),
                "summary", truncate(request.summary(), SUMMARY_MAX_LENGTH),
                "description", toAdf(request.description())));
        try {
            String response = http.post()
                    .uri(baseUrl + "/rest/api/3/issue")
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            String key = readKey(response);
            return new JiraIssue(key, baseUrl + "/browse/" + key);
        } catch (RestClientResponseException e) {
            throw new JiraException("Jira issue creation failed: " + e.getStatusCode() + " "
                    + e.getResponseBodyAsString(), e);
        } catch (RuntimeException e) {
            throw new JiraException("Jira issue creation failed: " + e.getMessage(), e);
        }
    }

    private static String readKey(String response) {
        try {
            JsonNode key = MAPPER.readTree(response).path("key");
            if (key.isMissingNode() || key.asText(null) == null) {
                throw new JiraException("Jira response missing 'key': " + response);
            }
            return key.asText();
        } catch (JiraException e) {
            throw e;
        } catch (Exception e) {
            throw new JiraException("Could not parse Jira response: " + e.getMessage(), e);
        }
    }

    /** Wrap plain text into a minimal ADF doc: one paragraph per non-blank line. */
    private static Map<String, Object> toAdf(String text) {
        List<Map<String, Object>> paragraphs = new ArrayList<>();
        if (text != null) {
            for (String line : text.split("\n")) {
                if (line.isBlank()) {
                    continue;
                }
                paragraphs.add(Map.of(
                        "type", "paragraph",
                        "content", List.of(Map.of("type", "text", "text", line))));
            }
        }
        if (paragraphs.isEmpty()) {
            paragraphs.add(Map.of("type", "paragraph", "content", List.of()));
        }
        return Map.of("type", "doc", "version", 1, "content", paragraphs);
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private static String stripTrailingSlash(String url) {
        return url != null && url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
