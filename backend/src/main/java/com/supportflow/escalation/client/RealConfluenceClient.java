package com.supportflow.escalation.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Production Confluence Cloud client. Creates a page via {@code POST /rest/api/content} (Basic auth,
 * shared Atlassian token), with the body in {@code storage} (XHTML) representation. The configured
 * base URL is expected to include the {@code /wiki} context path (e.g.
 * {@code https://your-site.atlassian.net/wiki}).
 *
 * <p>Not retried — page creation is not idempotent (see {@link RealJiraClient}). Failures surface as
 * {@link ConfluenceException}.
 */
@Component
@ConditionalOnProperty(name = "supportflow.integrations.client", havingValue = "real", matchIfMissing = true)
public class RealConfluenceClient implements ConfluenceClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RestClient http;
    private final String baseUrl;
    private final String authHeader;

    public RealConfluenceClient(
            RestClient.Builder builder,
            @Value("${supportflow.integrations.confluence.base-url:}") String baseUrl,
            @Value("${supportflow.integrations.atlassian.email:}") String email,
            @Value("${supportflow.integrations.atlassian.api-token:}") String apiToken) {
        this.http = builder.build();
        this.baseUrl = stripTrailingSlash(baseUrl);
        this.authHeader = AtlassianBasicAuth.header(email, apiToken);
    }

    @Override
    public ConfluencePage createKnownIssue(ConfluencePageRequest request) {
        Map<String, Object> body = Map.of(
                "type", "page",
                "title", request.title(),
                "space", Map.of("key", request.spaceKey()),
                "body", Map.of("storage", Map.of(
                        "value", toStorage(request.bodyText()),
                        "representation", "storage")));
        try {
            String response = http.post()
                    .uri(baseUrl + "/rest/api/content")
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return parsePage(response);
        } catch (RestClientResponseException e) {
            throw new ConfluenceException("Confluence page creation failed: " + e.getStatusCode() + " "
                    + e.getResponseBodyAsString(), e);
        } catch (RuntimeException e) {
            throw new ConfluenceException("Confluence page creation failed: " + e.getMessage(), e);
        }
    }

    private ConfluencePage parsePage(String response) {
        try {
            JsonNode root = MAPPER.readTree(response);
            String id = root.path("id").asText(null);
            if (id == null) {
                throw new ConfluenceException("Confluence response missing 'id': " + response);
            }
            JsonNode links = root.path("_links");
            String webui = links.path("webui").asText("");
            String linkBase = links.path("base").asText(baseUrl);
            String url = webui.isBlank() ? linkBase : linkBase + webui;
            return new ConfluencePage(id, url);
        } catch (ConfluenceException e) {
            throw e;
        } catch (Exception e) {
            throw new ConfluenceException("Could not parse Confluence response: " + e.getMessage(), e);
        }
    }

    /** Render plain text into storage-format XHTML: a status header plus one paragraph per line. */
    private static String toStorage(String text) {
        StringBuilder sb = new StringBuilder("<p><strong>Status:</strong> Investigating</p>");
        if (text != null) {
            for (String line : text.split("\n")) {
                if (line.isBlank()) {
                    continue;
                }
                sb.append("<p>").append(escape(line)).append("</p>");
            }
        }
        return sb.toString();
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private static String stripTrailingSlash(String url) {
        return url != null && url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
